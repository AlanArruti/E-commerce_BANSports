import { ChangeDetectionStrategy, Component, OnInit, computed, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  AbstractControl,
  FormArray,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { forkJoin } from 'rxjs';
import { Producto } from '../../models/producto';
import { STOCK_BAJO, Variante, atributosOrdenados, etiquetaAtributo } from '../../models/variante';
import { OperacionStock } from '../../models/variante-request';
import { ProductoService } from '../../services/producto.service';
import { ErrorTraducido, traducirErrorProducto, traducirErrorVariante } from '../../validaciones/errores-api';
import { leerPrecio, precioValido, requerido } from '../../validaciones/validadores';
import { PrecioPipe, formatearPrecio } from '../../pipes/precio.pipe';

type FilaAtributo = FormGroup<{ clave: FormControl<string>; valor: FormControl<string> }>;

const LARGO_ATRIBUTO = 50;

/** Claves de atributo repetidas (sin distinguir mayúsculas): "sabor" y "Sabor" son la misma. */
function sinClavesRepetidas(control: AbstractControl): ValidationErrors | null {
  const filas = (control as FormArray<FilaAtributo>).controls;
  const vistas = new Set<string>();
  for (const fila of filas) {
    const clave = fila.controls.clave.value.trim().toLowerCase();
    if (!clave) continue;
    if (vistas.has(clave)) return { claveRepetida: clave };
    vistas.add(clave);
  }
  return null;
}

/** Entero ≥ min, escrito solo con dígitos. */
function entero(min: number) {
  return (control: AbstractControl): ValidationErrors | null => {
    const valor = String(control.value ?? '').trim();
    if (!valor) return { required: true };
    if (!/^\d+$/.test(valor)) return { entero: true };
    return Number(valor) < min ? { min: { min } } : null;
  };
}

/**
 * Variantes y stock de un producto: /admin/productos/:id/variantes.
 * SCRUM-116 (alta, edición y baja de variantes con atributos libres) y
 * SCRUM-117 (ajuste rápido de stock y aviso de stock bajo).
 */
@Component({
  selector: 'app-admin-variantes',
  imports: [RouterLink, ReactiveFormsModule, NgClass, PrecioPipe],
  templateUrl: './admin-variantes.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './admin-variantes.css',
  host: { '(document:keydown.escape)': 'cerrarModales()' }
})
export class AdminVariantes implements OnInit {
  protected readonly stockBajo = STOCK_BAJO;
  protected readonly etiqueta = etiquetaAtributo;
  protected readonly ordenados = atributosOrdenados;

  protected readonly producto = signal<Producto | null>(null);
  protected readonly variantes = signal<Variante[]>([]);
  protected readonly cargando = signal(true);
  protected readonly noExiste = signal(false);
  protected readonly errorCarga = signal<ErrorTraducido | null>(null);
  protected readonly aviso = signal<string | null>(null);

  protected readonly stockTotal = computed(() => this.variantes().reduce((total, v) => total + v.stock, 0));
  protected readonly cantidadStockBajo = computed(
    () => this.variantes().filter((v) => v.stock <= STOCK_BAJO).length
  );

  // ── Formulario de alta / edición ───────────────────────
  /** null = formulario cerrado; 'nueva' = alta; una variante = edición. */
  protected readonly editando = signal<Variante | 'nueva' | null>(null);
  protected readonly guardando = signal(false);
  protected readonly intentoGuardar = signal(false);
  protected readonly errorForm = signal<ErrorTraducido | null>(null);

  atributos = new FormArray<FilaAtributo>([], { validators: [sinClavesRepetidas] });
  stock = new FormControl('0', { nonNullable: true, validators: [entero(0)] });
  /** Precio propio de la variante (SCRUM-154). Vacío = cuesta lo mismo que el producto. */
  precio = new FormControl('', { nonNullable: true, validators: [precioValido] });
  formulario = new FormGroup({ atributos: this.atributos, stock: this.stock, precio: this.precio });

  // ── Ajuste de stock ────────────────────────────────────
  protected readonly ajustando = signal<Variante | null>(null);
  protected readonly enviandoAjuste = signal(false);
  protected readonly intentoAjuste = signal(false);
  protected readonly errorAjuste = signal<ErrorTraducido | null>(null);
  protected readonly operacion = signal<OperacionStock>('AGREGAR');
  cantidad = new FormControl('', { nonNullable: true, validators: [entero(1)] });
  ajusteForm = new FormGroup({ cantidad: this.cantidad });

  // ── Baja ───────────────────────────────────────────────
  protected readonly bajando = signal<Variante | null>(null);
  protected readonly dandoDeBaja = signal(false);
  protected readonly errorBaja = signal<ErrorTraducido | null>(null);

  private idProducto = 0;

  constructor(
    private route: ActivatedRoute,
    private productoService: ProductoService,
    private title: Title
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.idProducto = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isInteger(this.idProducto) || this.idProducto <= 0) {
      this.cargando.set(false);
      this.noExiste.set(true);
      return;
    }

    this.cargando.set(true);
    this.errorCarga.set(null);
    forkJoin({
      producto: this.productoService.obtener(this.idProducto),
      variantes: this.productoService.listarVariantes(this.idProducto)
    }).subscribe({
      next: ({ producto, variantes }) => {
        this.producto.set(producto);
        this.variantes.set([...variantes].sort((a, b) => a.id - b.id));
        this.cargando.set(false);
        this.title.setTitle(`Variantes de ${producto.nombre} | Panel BAN Sports`);
      },
      error: (e: unknown) => {
        this.cargando.set(false);
        if (e instanceof HttpErrorResponse && e.status === 404) {
          this.noExiste.set(true);
        } else if (e instanceof HttpErrorResponse) {
          this.errorCarga.set(traducirErrorProducto(e));
        } else {
          this.errorCarga.set({ titulo: 'No pudimos cargar el producto', detalle: 'Intentá de nuevo.' });
        }
      }
    });
  }

  protected nivelStock(stock: number): 'cero' | 'bajo' | 'ok' {
    if (stock === 0) return 'cero';
    return stock <= STOCK_BAJO ? 'bajo' : 'ok';
  }

  protected descripcion(variante: Variante): string {
    return this.ordenados(variante)
      .map(([, valor]) => valor)
      .join(' · ');
  }

  // ── Alta / edición ─────────────────────────────────────

  nueva(): void {
    this.abrirFormulario('nueva', [['', '']], '0', '');
  }

  editar(variante: Variante): void {
    const precio = variante.precio != null ? formatearPrecio(variante.precio).replace('$ ', '') : '';
    this.abrirFormulario(variante, this.ordenados(variante), String(variante.stock), precio);
  }

  agregarAtributo(): void {
    this.atributos.push(this.filaAtributo('', ''));
  }

  quitarAtributo(indice: number): void {
    if (this.atributos.length > 1) {
      this.atributos.removeAt(indice);
    }
  }

  cancelarFormulario(): void {
    if (!this.guardando()) {
      this.editando.set(null);
    }
  }

  /** Mensaje de un campo del formulario, solo si ya lo tocaron o se intentó guardar. */
  protected errorCampo(control: AbstractControl, campo: 'clave' | 'valor' | 'stock' | 'cantidad' | 'precio'): string | null {
    const intento = campo === 'cantidad' ? this.intentoAjuste() : this.intentoGuardar();
    if (!control.errors || (!control.touched && !intento)) return null;
    if (control.errors['requerido'] || control.errors['required']) {
      return { clave: 'Poné el nombre del atributo.', valor: 'Poné el valor.', stock: 'Poné el stock.', cantidad: 'Poné una cantidad.', precio: 'Poné el precio.' }[campo];
    }
    if (control.errors['precioFormato']) return 'Usá solo números, puntos para los miles y coma para los centavos (por ejemplo 42.999,99).';
    if (control.errors['precioCero']) return 'El precio tiene que ser mayor a $ 0.';
    if (control.errors['maxlength']) return `Máximo ${LARGO_ATRIBUTO} caracteres.`;
    if (control.errors['entero']) return 'Solo números enteros, sin puntos ni comas.';
    if (control.errors['min']) return campo === 'cantidad' ? 'Tiene que ser al menos 1.' : 'No puede ser negativo.';
    return null;
  }

  protected claveRepetida(): string | null {
    const repetida = this.atributos.errors?.['claveRepetida'] as string | undefined;
    return repetida ? `El atributo "${repetida}" está repetido.` : null;
  }

  guardar(): void {
    this.intentoGuardar.set(true);
    this.errorForm.set(null);
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    const atributos: Record<string, string> = {};
    for (const fila of this.atributos.controls) {
      atributos[fila.controls.clave.value.trim().toLowerCase()] = fila.controls.valor.value.trim();
    }
    const precioTexto = this.precio.value.trim();
    const cuerpo = { atributos, stock: Number(this.stock.value), precio: precioTexto ? leerPrecio(precioTexto) : null };
    const actual = this.editando();
    const pedido =
      actual === 'nueva' || actual === null
        ? this.productoService.crearVariante(this.idProducto, cuerpo)
        : this.productoService.actualizarVariante(this.idProducto, actual.id, cuerpo);

    this.guardando.set(true);
    pedido.subscribe({
      next: (variante) => {
        this.guardando.set(false);
        this.editando.set(null);
        this.reemplazar(variante);
        this.aviso.set(actual === 'nueva' ? 'Variante agregada.' : 'Variante actualizada.');
      },
      error: (e: unknown) => {
        this.guardando.set(false);
        this.errorForm.set(this.traducir(e));
      }
    });
  }

  // ── Ajuste de stock ────────────────────────────────────

  abrirAjuste(variante: Variante): void {
    this.ajustando.set(variante);
    this.operacion.set('AGREGAR');
    this.cantidad.reset('');
    this.intentoAjuste.set(false);
    this.errorAjuste.set(null);
  }

  cerrarAjuste(): void {
    if (!this.enviandoAjuste()) {
      this.ajustando.set(null);
    }
  }

  /** Stock que quedaría con la cantidad escrita (null si todavía no es un número válido). */
  protected stockResultante(): number | null {
    const variante = this.ajustando();
    if (!variante || this.cantidad.invalid) return null;
    const cantidad = Number(this.cantidad.value);
    return this.operacion() === 'AGREGAR' ? variante.stock + cantidad : variante.stock - cantidad;
  }

  confirmarAjuste(): void {
    const variante = this.ajustando();
    this.intentoAjuste.set(true);
    this.errorAjuste.set(null);
    if (!variante || this.cantidad.invalid) {
      this.cantidad.markAsTouched();
      return;
    }

    this.enviandoAjuste.set(true);
    this.productoService
      .ajustarStock(this.idProducto, variante.id, { operacion: this.operacion(), cantidad: Number(this.cantidad.value) })
      .subscribe({
        next: (actualizada) => {
          this.enviandoAjuste.set(false);
          this.ajustando.set(null);
          this.reemplazar(actualizada);
          this.aviso.set(`Stock de ${this.descripcion(actualizada)} actualizado: ${actualizada.stock} u.`);
        },
        error: (e: unknown) => {
          this.enviandoAjuste.set(false);
          this.errorAjuste.set(this.traducir(e));
        }
      });
  }

  // ── Baja ───────────────────────────────────────────────

  pedirBaja(variante: Variante): void {
    this.bajando.set(variante);
    this.errorBaja.set(null);
  }

  cerrarBaja(): void {
    if (!this.dandoDeBaja()) {
      this.bajando.set(null);
    }
  }

  confirmarBaja(): void {
    const variante = this.bajando();
    if (!variante) return;
    this.dandoDeBaja.set(true);
    this.errorBaja.set(null);
    this.productoService.eliminarVariante(this.idProducto, variante.id).subscribe({
      next: () => {
        this.dandoDeBaja.set(false);
        this.bajando.set(null);
        this.variantes.update((lista) => lista.filter((v) => v.id !== variante.id));
        if (this.editando() !== 'nueva' && (this.editando() as Variante | null)?.id === variante.id) {
          this.editando.set(null);
        }
        this.aviso.set('Variante dada de baja.');
      },
      error: (e: unknown) => {
        this.dandoDeBaja.set(false);
        this.errorBaja.set(this.traducir(e));
      }
    });
  }

  cerrarModales(): void {
    this.cerrarAjuste();
    this.cerrarBaja();
  }

  // ── Internos ───────────────────────────────────────────

  private abrirFormulario(modo: Variante | 'nueva', atributos: [string, string][], stock: string, precio: string): void {
    this.atributos.clear();
    for (const [clave, valor] of atributos) {
      this.atributos.push(this.filaAtributo(clave, valor));
    }
    this.stock.reset(stock);
    this.precio.reset(precio);
    this.intentoGuardar.set(false);
    this.errorForm.set(null);
    this.aviso.set(null);
    this.editando.set(modo);
  }

  private filaAtributo(clave: string, valor: string): FilaAtributo {
    return new FormGroup({
      clave: new FormControl(clave, {
        nonNullable: true,
        validators: [requerido, Validators.maxLength(LARGO_ATRIBUTO)]
      }),
      valor: new FormControl(valor, {
        nonNullable: true,
        validators: [requerido, Validators.maxLength(LARGO_ATRIBUTO)]
      })
    });
  }

  /** Pone la variante devuelta por el back en la lista (la agrega si es nueva), sin recargar. */
  private reemplazar(variante: Variante): void {
    this.variantes.update((lista) => {
      const sinEsta = lista.filter((v) => v.id !== variante.id);
      return [...sinEsta, variante].sort((a, b) => a.id - b.id);
    });
  }

  private traducir(e: unknown): ErrorTraducido {
    return e instanceof HttpErrorResponse
      ? traducirErrorVariante(e)
      : { titulo: 'Algo salió mal', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' };
  }
}
