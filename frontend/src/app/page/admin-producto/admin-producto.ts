import { Component, DestroyRef, OnInit, computed, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of, switchMap } from 'rxjs';
import { PoteProducto, abreviarNombre } from '../../componente/pote-producto/pote-producto';
import { Categoria } from '../../models/categoria';
import { Producto } from '../../models/producto';
import { PrecioPipe, formatearPrecio } from '../../pipes/precio.pipe';
import { CategoriaService } from '../../services/categoria.service';
import { ImagenProductoService } from '../../services/imagen-producto.service';
import { ProductoService } from '../../services/producto.service';
import { ErrorTraducido, traducirErrorGuardarProducto, traducirErrorProducto } from '../../validaciones/errores-api';
import { mensajeDeError } from '../../validaciones/mensajes';
import { descuentoValido, leerPrecio, precioValido, requerido } from '../../validaciones/validadores';
import {
  EXTENSIONES_IMAGEN,
  formatearTamanio,
  traducirErrorImagen,
  validarImagen
} from '../../validaciones/imagen-producto';

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

  // ── Foto del alta: se elige antes de crear y se sube apenas el producto existe ──
  protected readonly extensionesImagen = EXTENSIONES_IMAGEN;
  protected readonly foto = signal<File | null>(null);
  /** URL local (blob:) para mostrar la foto elegida antes de subirla. */
  protected readonly fotoUrl = signal<string | null>(null);
  protected readonly errorFoto = signal<string | null>(null);
  /** El producto se creó pero la foto no subió: se avisa en "Producto creado". */
  protected readonly avisoFoto = signal<string | null>(null);
  protected readonly tamanioFoto = computed(() => {
    const archivo = this.foto();
    return archivo ? formatearTamanio(archivo.size) : '';
  });

  // Mismas reglas que ProductoRequest en el back.
  nombre = new FormControl('', { nonNullable: true, validators: [requerido, Validators.maxLength(100)] });
  categoria = new FormControl<number | null>(null, { validators: [Validators.required] });
  precio = new FormControl('', { nonNullable: true, validators: [requerido, precioValido] });
  descuento = new FormControl('', { nonNullable: true, validators: [descuentoValido] });
  descripcion = new FormControl('', { nonNullable: true, validators: [requerido, Validators.maxLength(250)] });
  /** Mostrarlo en "Novedades" del inicio. */
  novedad = new FormControl(false, { nonNullable: true });

  formulario = new FormGroup({
    nombre: this.nombre,
    categoria: this.categoria,
    precio: this.precio,
    descuento: this.descuento,
    descripcion: this.descripcion,
    novedad: this.novedad
  });

  /** Imagen que ya tiene el producto (solo en la edición). */
  protected readonly imagenActual = signal<string | null>(null);

  /** Vista previa de la tarjeta del catálogo, en vivo mientras se escribe. */
  protected readonly vistaPrevia;

  constructor(
    private productoService: ProductoService,
    private imagenProductoService: ImagenProductoService,
    private categoriaService: CategoriaService,
    private router: Router,
    private route: ActivatedRoute,
    private title: Title
  ) {
    // La URL local de la foto se libera al salir de la pantalla.
    inject(DestroyRef).onDestroy(() => this.soltarFotoUrl());

    const id = this.route.snapshot.paramMap.get('id');
    this.idEditado = id === null ? null : Number(id);

    const valores = toSignal(this.formulario.valueChanges, { initialValue: this.formulario.getRawValue() });
    this.vistaPrevia = computed(() => {
      const { nombre, categoria, precio, descuento, descripcion } = valores();
      const precioLeido = leerPrecio(precio ?? '');
      const porcentaje = Number(descuento ?? '');
      const descuentoAplicado = Number.isInteger(porcentaje) && porcentaje >= 1 && porcentaje <= 99
        ? porcentaje
        : null;
      const precioFinal = precioLeido > 0
        ? (descuentoAplicado === null
            ? precioLeido
            : Math.round(precioLeido * (100 - descuentoAplicado)) / 100)
        : null;
      return {
        nombre: (nombre ?? '').trim(),
        abreviatura: abreviarNombre((nombre ?? '').trim()) || '—',
        categoria: this.categorias().find((opcion) => opcion.id === categoria)?.nombre ?? null,
        precio: precioLeido > 0 ? precioLeido : null,
        descuento: descuentoAplicado,
        precioFinal,
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
      descuento: this.descuento.value.trim() === '' ? null : Number(this.descuento.value.trim()),
      categoriaId: this.categoria.value!,
      novedad: this.novedad.value
    };

    this.guardando.set(true);
    const foto = this.foto();
    const pedido =
      this.idEditado === null
        ? this.productoService.crear(datos).pipe(switchMap((producto) => this.subirFoto(producto, foto)))
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

  /**
   * Sube la foto elegida al producto recién creado. Si falla, el producto ya existe igual:
   * se avisa el motivo en "Producto creado" y se puede cargar desde "Cargar imagen".
   */
  private subirFoto(producto: Producto, foto: File | null) {
    this.avisoFoto.set(null);
    if (!foto) {
      return of(producto);
    }
    return this.imagenProductoService.subir(producto.id, foto).pipe(
      map((imagen): Producto => ({ ...producto, imagenUrl: imagen.url })),
      catchError((e: unknown) => {
        const motivo = e instanceof HttpErrorResponse ? traducirErrorImagen(e, 'subir').detalle : 'Ocurrió un error inesperado.';
        this.avisoFoto.set(`El producto se creó, pero la foto no se pudo subir: ${motivo}`);
        return of(producto);
      })
    );
  }

  /** Foto elegida en el alta: se valida igual que en la pantalla de imagen y se muestra en la vista previa. */
  elegirFoto(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files?.[0] ?? null;
    input.value = ''; // Así se puede volver a elegir el mismo archivo después de quitarlo.
    if (!archivo) {
      return;
    }
    const problema = validarImagen(archivo);
    this.errorFoto.set(problema);
    if (problema) {
      return;
    }
    this.soltarFotoUrl();
    this.foto.set(archivo);
    this.fotoUrl.set(URL.createObjectURL(archivo));
  }

  quitarFoto(): void {
    this.soltarFotoUrl();
    this.foto.set(null);
    this.errorFoto.set(null);
  }

  private soltarFotoUrl(): void {
    const url = this.fotoUrl();
    if (url) {
      URL.revokeObjectURL(url);
    }
    this.fotoUrl.set(null);
  }

  /** Desde "Producto creado": formulario vacío para cargar otro. */
  crearOtro(): void {
    this.creado.set(null);
    this.intentoGuardar.set(false);
    this.quitarFoto();
    this.avisoFoto.set(null);
    this.formulario.reset({ nombre: '', categoria: null, precio: '', descuento: '', descripcion: '', novedad: false });
    this.title.setTitle('Nuevo producto | Panel BAN Sports');
  }

  private precargar(producto: Producto): void {
    this.formulario.reset({
      nombre: producto.nombre,
      categoria: producto.categoria?.id ?? null,
      // Mismo formato que se escribe: "42.999,99".
      precio: formatearPrecio(producto.precioBase).replace('$ ', ''),
      descuento: producto.descuento ? String(producto.descuento) : '',
      descripcion: producto.descripcion,
      novedad: producto.novedad
    });
    // En la edición la vista previa muestra la imagen que ya tiene (se cambia desde "Imagen").
    this.imagenActual.set(producto.imagenUrl);
    this.intentoGuardar.set(false);
  }
}
