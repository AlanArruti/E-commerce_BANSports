import { Component, OnInit, computed, signal, ChangeDetectionStrategy } from '@angular/core';
import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { PoteProducto, abreviarNombre } from '../../componente/pote-producto/pote-producto';
import { Categoria } from '../../models/categoria';
import { Producto } from '../../models/producto';
import { PrecioPipe, formatearPrecio } from '../../pipes/precio.pipe';
import { CategoriaService } from '../../services/categoria.service';
import { ProductoService } from '../../services/producto.service';
import { ErrorTraducido, traducirErrorGuardarProducto, traducirErrorProducto } from '../../validaciones/errores-api';
import { mensajeDeError } from '../../validaciones/mensajes';
import { leerPrecio, precioValido, requerido } from '../../validaciones/validadores';

const ERROR_INESPERADO: ErrorTraducido = {
  titulo: 'No pudimos guardar el producto',
  detalle: 'Ocurrió un error inesperado. Intentá de nuevo.'
};

/**
 * Alta y edición de productos del panel (SCRUM-114).
 * /admin/productos/nuevo crea; /admin/productos/:id/editar edita.
 */
@Component({
  selector: 'app-admin-producto',
  imports: [NgClass, ReactiveFormsModule, RouterLink, PoteProducto, PrecioPipe],
  templateUrl: './admin-producto.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './admin-producto.css'
})
export class AdminProducto implements OnInit {
  /** null = alta. */
  protected readonly idEditado: number | null;

  protected readonly cargando = signal(true);
  protected readonly errorCarga = signal<ErrorTraducido | null>(null);
  protected readonly noEncontrado = signal(false);
  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly guardando = signal(false);
  protected readonly intentoGuardar = signal(false);
  protected readonly errorGuardar = signal<ErrorTraducido | null>(null);
  /** Producto recién creado: se muestra la pantalla de próximos pasos. */
  protected readonly creado = signal<Producto | null>(null);

  // Mismas reglas que ProductoRequest en el back.
  nombre = new FormControl('', { nonNullable: true, validators: [requerido, Validators.maxLength(100)] });
  categoria = new FormControl<number | null>(null, { validators: [Validators.required] });
  precio = new FormControl('', { nonNullable: true, validators: [requerido, precioValido] });
  descripcion = new FormControl('', { nonNullable: true, validators: [requerido, Validators.maxLength(250)] });

  formulario = new FormGroup({
    nombre: this.nombre,
    categoria: this.categoria,
    precio: this.precio,
    descripcion: this.descripcion
  });

  /** Vista previa de la tarjeta del catálogo, en vivo mientras se escribe. */
  protected readonly vistaPrevia;

  constructor(
    private productoService: ProductoService,
    private categoriaService: CategoriaService,
    private router: Router,
    private route: ActivatedRoute,
    private title: Title
  ) {
    const id = this.route.snapshot.paramMap.get('id');
    this.idEditado = id === null ? null : Number(id);

    const valores = toSignal(this.formulario.valueChanges, { initialValue: this.formulario.getRawValue() });
    this.vistaPrevia = computed(() => {
      const { nombre, categoria, precio, descripcion } = valores();
      const precioLeido = leerPrecio(precio ?? '');
      return {
        nombre: (nombre ?? '').trim(),
        abreviatura: abreviarNombre((nombre ?? '').trim()) || '—',
        categoria: this.categorias().find((opcion) => opcion.id === categoria)?.nombre ?? null,
        precio: precioLeido > 0 ? precioLeido : null,
        largoNombre: (nombre ?? '').trim().length,
        largoDescripcion: (descripcion ?? '').trim().length
      };
    });
  }

  ngOnInit(): void {
    this.cargar();
  }

  /** Trae las categorías para el select y, si es edición, el producto a precargar. */
  cargar(): void {
    this.cargando.set(true);
    this.errorCarga.set(null);
    const idEditado = this.idEditado;
    const valido = idEditado === null || (Number.isInteger(idEditado) && idEditado > 0);
    if (!valido) {
      this.cargando.set(false);
      this.noEncontrado.set(true);
      return;
    }

    forkJoin([
      this.categoriaService.listarActualizadas(),
      idEditado === null ? of(null) : this.productoService.obtener(idEditado)
    ]).subscribe({
      next: ([categorias, producto]) => {
        this.cargando.set(false);
        this.categorias.set(categorias);
        if (producto) {
          this.precargar(producto);
          this.title.setTitle(`Editar ${producto.nombre} | Panel BAN Sports`);
        }
      },
      error: (e: unknown) => {
        this.cargando.set(false);
        if (e instanceof HttpErrorResponse && e.status === 404) {
          this.noEncontrado.set(true);
          return;
        }
        this.errorCarga.set(
          e instanceof HttpErrorResponse
            ? traducirErrorProducto(e)
            : { titulo: 'No pudimos cargar el formulario', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
      }
    });
  }

  /** Mensaje de error del campo, solo si ya lo tocó o intentó guardar. */
  error(control: FormControl, campo: string): string | null {
    if (!control.touched && !this.intentoGuardar()) {
      return null;
    }
    return mensajeDeError(control, campo);
  }

  guardar(): void {
    this.intentoGuardar.set(true);
    this.errorGuardar.set(null);
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    const datos = {
      nombre: this.nombre.value.trim(),
      descripcion: this.descripcion.value.trim(),
      precioBase: leerPrecio(this.precio.value),
      categoriaId: this.categoria.value!
    };

    this.guardando.set(true);
    const pedido =
      this.idEditado === null
        ? this.productoService.crear(datos)
        : this.productoService.actualizar(this.idEditado, datos);
    pedido.subscribe({
      next: (producto) => {
        this.guardando.set(false);
        if (this.idEditado === null) {
          this.creado.set(producto);
          this.title.setTitle(`Producto creado | Panel BAN Sports`);
          window.scrollTo({ top: 0 });
        } else {
          // Vuelve al listado; el mensaje viaja en el estado de la navegación.
          this.router.navigate(['/admin/productos'], {
            state: { aviso: `Guardaste los cambios de "${producto.nombre}".` }
          });
        }
      },
      error: (e: unknown) => {
        this.guardando.set(false);
        this.errorGuardar.set(e instanceof HttpErrorResponse ? traducirErrorGuardarProducto(e) : ERROR_INESPERADO);
      }
    });
  }

  /** Desde "Producto creado": formulario vacío para cargar otro. */
  crearOtro(): void {
    this.creado.set(null);
    this.intentoGuardar.set(false);
    this.formulario.reset({ nombre: '', categoria: null, precio: '', descripcion: '' });
    this.title.setTitle('Nuevo producto | Panel BAN Sports');
  }

  private precargar(producto: Producto): void {
    this.formulario.reset({
      nombre: producto.nombre,
      categoria: producto.categoria?.id ?? null,
      // Mismo formato que se escribe: "42.999,99".
      precio: formatearPrecio(producto.precioBase).replace('$ ', ''),
      descripcion: producto.descripcion
    });
    this.intentoGuardar.set(false);
  }
}
