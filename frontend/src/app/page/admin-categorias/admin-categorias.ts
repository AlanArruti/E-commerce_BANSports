import { Component, ElementRef, OnInit, computed, effect, signal, viewChild, ChangeDetectionStrategy } from '@angular/core';
import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, map, of, catchError } from 'rxjs';
import { Categoria } from '../../models/categoria';
import { CategoriaService } from '../../services/categoria.service';
import { ProductoService } from '../../services/producto.service';
import { ErrorTraducido, traducirErrorCategoria } from '../../validaciones/errores-api';
import { mensajeDeError } from '../../validaciones/mensajes';
import { requerido } from '../../validaciones/validadores';

interface FilaCategoria {
  categoria: Categoria;
  /** Productos activos de la categoría. null mientras se cuentan o si no se pudo contar. */
  productos: number | null;
}

/** Modal abierto: crear o renombrar (categoria null = nueva), o eliminar. */
type Modal =
  | { tipo: 'formulario'; categoria: Categoria | null }
  | { tipo: 'eliminar'; categoria: Categoria; consultando: boolean; afectados: string[] };

const ERROR_INESPERADO: ErrorTraducido = {
  titulo: 'No pudimos guardar los cambios',
  detalle: 'Ocurrió un error inesperado. Intentá de nuevo.'
};

/** Para comparar nombres sin importar mayúsculas ni espacios de más. */
function normalizar(nombre: string): string {
  return nombre.trim().toLowerCase();
}

/** "id 3 — Whey Protein Isolate" (formato del back) → "Whey Protein Isolate". */
function nombreDeProducto(aviso: string): string {
  return aviso.replace(/^id \d+ — /, '');
}

/** ABM de categorías del panel (SCRUM-112). */
@Component({
  selector: 'app-admin-categorias',
  imports: [NgClass, ReactiveFormsModule, RouterLink],
  templateUrl: './admin-categorias.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './admin-categorias.css',
  host: { '(document:keydown.escape)': 'cerrarModal()' }
})
export class AdminCategorias implements OnInit {
  protected readonly cargando = signal(true);
  protected readonly errorCarga = signal<ErrorTraducido | null>(null);
  protected readonly filas = signal<FilaCategoria[]>([]);
  /** Confirmación de la última acción ("Creaste la categoría…"). */
  protected readonly aviso = signal<string | null>(null);

  protected readonly modal = signal<Modal | null>(null);
  protected readonly guardando = signal(false);
  protected readonly errorModal = signal<ErrorTraducido | null>(null);
  protected readonly intentoGuardar = signal(false);

  busqueda = new FormControl('', { nonNullable: true });
  // Mismas reglas que CategoriaRequest en el back, más el nombre repetido (sin distinguir mayúsculas).
  nombre = new FormControl('', {
    nonNullable: true,
    validators: [requerido, Validators.maxLength(50), (control) => this.nombreRepetido(control)]
  });
  /** Sin [formGroup], el <form> haría un submit nativo y recargaría la página. */
  formulario = new FormGroup({ nombre: this.nombre });

  protected readonly filasVisibles;
  protected readonly largoNombre;

  private readonly campoNombre = viewChild<ElementRef<HTMLInputElement>>('campoNombre');

  constructor(
    private categoriaService: CategoriaService,
    private productoService: ProductoService
  ) {
    const textoBuscado = toSignal(this.busqueda.valueChanges, { initialValue: '' });
    this.filasVisibles = computed(() => {
      const buscado = normalizar(textoBuscado());
      return this.filas().filter((fila) => normalizar(fila.categoria.nombre).includes(buscado));
    });

    const valorNombre = toSignal(this.nombre.valueChanges, { initialValue: '' });
    this.largoNombre = computed(() => valorNombre().trim().length);

    // Al abrir el formulario, el cursor va directo al nombre.
    effect(() => this.campoNombre()?.nativeElement.focus());
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.errorCarga.set(null);
    this.categoriaService.listarActualizadas().subscribe({
      next: (categorias) => {
        this.cargando.set(false);
        this.filas.set(categorias.map((categoria) => ({ categoria, productos: null })));
        this.contarProductos(categorias);
      },
      error: (e: unknown) => {
        this.cargando.set(false);
        this.errorCarga.set(e instanceof HttpErrorResponse ? traducirErrorCategoria(e) : ERROR_INESPERADO);
      }
    });
  }

  abrirFormulario(categoria: Categoria | null): void {
    this.aviso.set(null);
    this.errorModal.set(null);
    this.intentoGuardar.set(false);
    // Primero el modal: el validador de repetidos lo usa para no comparar la categoría consigo misma.
    this.modal.set({ tipo: 'formulario', categoria });
    this.nombre.reset(categoria?.nombre ?? '');
  }

  /** Primero le pregunta al back qué productos quedarían sin categoría, y recién después confirma. */
  abrirEliminar(categoria: Categoria): void {
    this.aviso.set(null);
    this.errorModal.set(null);
    this.modal.set({ tipo: 'eliminar', categoria, consultando: true, afectados: [] });
    this.categoriaService.eliminar(categoria.id, false).subscribe({
      next: (respuesta) =>
        this.modal.set({
          tipo: 'eliminar',
          categoria,
          consultando: false,
          afectados: respuesta.aviso.map(nombreDeProducto)
        }),
      error: (e: unknown) => {
        this.modal.set({ tipo: 'eliminar', categoria, consultando: false, afectados: [] });
        this.mostrarErrorModal(e);
      }
    });
  }

  cerrarModal(): void {
    if (!this.guardando()) {
      this.modal.set(null);
    }
  }

  /** Mensaje de error del nombre, solo si ya lo tocó o intentó guardar. */
  errorNombre(): string | null {
    if (!this.nombre.touched && !this.intentoGuardar()) {
      return null;
    }
    return mensajeDeError(this.nombre, 'categoriaNombre');
  }

  guardar(): void {
    const modal = this.modal();
    if (modal?.tipo !== 'formulario') {
      return;
    }
    this.intentoGuardar.set(true);
    this.errorModal.set(null);
    if (this.nombre.invalid) {
      this.nombre.markAsTouched();
      return;
    }

    const nombre = this.nombre.value.trim();
    const original = modal.categoria;
    if (original && original.nombre === nombre) {
      this.modal.set(null);
      return;
    }

    this.guardando.set(true);
    const pedido = original
      ? this.categoriaService.renombrar(original.id, nombre)
      : this.categoriaService.crear(nombre);
    pedido.subscribe({
      next: (categoria) => {
        this.guardando.set(false);
        this.modal.set(null);
        if (original) {
          this.filas.update((filas) =>
            filas.map((fila) => (fila.categoria.id === categoria.id ? { ...fila, categoria } : fila))
          );
          this.aviso.set(`Renombraste "${original.nombre}" a "${categoria.nombre}".`);
        } else {
          this.filas.update((filas) => [...filas, { categoria, productos: 0 }]);
          this.aviso.set(`Creaste la categoría "${categoria.nombre}".`);
        }
      },
      error: (e: unknown) => {
        this.guardando.set(false);
        if (e instanceof HttpErrorResponse && e.status === 409) {
          // El error queda pegado al campo hasta que cambie el nombre.
          this.nombre.setErrors({ servidor: `Ya existe una categoría llamada "${nombre}".` });
          return;
        }
        this.mostrarErrorModal(e);
      }
    });
  }

  confirmarEliminar(): void {
    const modal = this.modal();
    if (modal?.tipo !== 'eliminar') {
      return;
    }
    this.guardando.set(true);
    this.errorModal.set(null);
    this.categoriaService.eliminar(modal.categoria.id, true).subscribe({
      next: () => {
        this.guardando.set(false);
        this.modal.set(null);
        this.filas.update((filas) => filas.filter((fila) => fila.categoria.id !== modal.categoria.id));
        const sinCategoria = modal.afectados.length;
        this.aviso.set(
          `Eliminaste la categoría "${modal.categoria.nombre}".` +
            (sinCategoria ? ` ${sinCategoria === 1 ? '1 producto quedó' : `${sinCategoria} productos quedaron`} sin categoría.` : '')
        );
      },
      error: (e: unknown) => {
        this.guardando.set(false);
        this.mostrarErrorModal(e);
      }
    });
  }

  /** Cuenta los productos de todas las categorías en paralelo. Si una falla, queda sin número. */
  private contarProductos(categorias: Categoria[]): void {
    if (!categorias.length) {
      return;
    }
    forkJoin(
      categorias.map((categoria) =>
        this.productoService.contarPorCategoria(categoria.id).pipe(
          map((productos) => ({ id: categoria.id, productos })),
          catchError(() => of({ id: categoria.id, productos: null }))
        )
      )
    ).subscribe((conteos) =>
      this.filas.update((filas) =>
        filas.map((fila) => ({
          ...fila,
          productos: conteos.find((conteo) => conteo.id === fila.categoria.id)?.productos ?? fila.productos
        }))
      )
    );
  }

  private nombreRepetido(control: AbstractControl): ValidationErrors | null {
    const nombre = normalizar(String(control.value ?? ''));
    const modal = this.modal?.();
    const idEditado = modal?.tipo === 'formulario' ? modal.categoria?.id : undefined;
    const repetido = this.filas?.().some(
      (fila) => fila.categoria.id !== idEditado && normalizar(fila.categoria.nombre) === nombre
    );
    return nombre && repetido ? { repetido: true } : null;
  }

  private mostrarErrorModal(e: unknown): void {
    this.errorModal.set(e instanceof HttpErrorResponse ? traducirErrorCategoria(e) : ERROR_INESPERADO);
    if (e instanceof HttpErrorResponse && e.status === 404) {
      this.cargar();
    }
  }
}
