import { Component, OnInit, computed, input, linkedSignal, output, signal, ChangeDetectionStrategy } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Estrellas } from '../estrellas/estrellas';
import { ErrorCarga } from '../error-carga/error-carga';
import { PermisoResena, Resena } from '../../models/resena';
import { PromedioPipe } from '../../pipes/promedio.pipe';
import { AuthService } from '../../services/auth.service';
import { ProductoService } from '../../services/producto.service';
import { ResenaService } from '../../services/resena.service';
import { ErrorTraducido, traducirErrorResena } from '../../validaciones/errores-api';
import { mensajeDeError } from '../../validaciones/mensajes';

const TAMANIO_PAGINA = 5;
const LARGO_COMENTARIO = 500;

/** Qué significa cada puntaje, para el formulario. */
const ETIQUETAS_ESTRELLAS = ['Elegí de 1 a 5', 'Malo', 'Regular', 'Bueno', 'Muy bueno', 'Excelente'];

export interface ResumenResenas {
  promedio: number;
  cantidad: number;
}

/**
 * Sección "Opiniones" del detalle de producto (SCRUM-150): resumen, formulario para
 * quien compró el producto, lista paginada y moderación del admin.
 */
@Component({
  selector: 'app-resenas-producto',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, Estrellas, ErrorCarga, PromedioPipe],
  templateUrl: './resenas-producto.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './resenas-producto.css',
  host: { '(document:keydown.escape)': 'cerrarBorrado()' }
})
export class ResenasProducto implements OnInit {
  readonly idProducto = input.required<number>();
  /** Lo que ya trae el producto, para no esperar al back. */
  readonly promedio = input(0);
  readonly cantidad = input(0);
  /** Avisa al detalle cuando cambia el promedio (después de opinar o de borrar). */
  readonly resumenCambio = output<ResumenResenas>();

  protected readonly resumen = linkedSignal<ResumenResenas>(() => ({
    promedio: this.promedio(),
    cantidad: this.cantidad()
  }));

  protected readonly resenas = signal<Resena[]>([]);
  /** La lista sin la reseña propia, que ya se muestra arriba como "Tu opinión". */
  protected readonly resenasDeOtros = computed(() => {
    const propia = this.permiso()?.resenaPropia;
    return propia ? this.resenas().filter((resena) => resena.id !== propia.id) : this.resenas();
  });
  protected readonly hayMas = signal(false);
  protected readonly cargando = signal(true);
  protected readonly cargandoMas = signal(false);
  protected readonly error = signal<ErrorTraducido | null>(null);
  protected readonly permiso = signal<PermisoResena | null>(null);

  // Formulario
  protected readonly estrellasElegidas = signal(0);
  protected readonly etiquetas = ETIQUETAS_ESTRELLAS;
  protected readonly largoMaximo = LARGO_COMENTARIO;
  comentario = new FormControl('', { nonNullable: true, validators: [Validators.maxLength(LARGO_COMENTARIO)] });
  protected readonly intentoPublicar = signal(false);
  protected readonly publicando = signal(false);
  protected readonly errorPublicar = signal<ErrorTraducido | null>(null);
  protected readonly recienPublicada = signal(false);

  // Moderación
  protected readonly resenaABorrar = signal<Resena | null>(null);
  protected readonly borrando = signal(false);
  protected readonly errorBorrar = signal<ErrorTraducido | null>(null);

  private pagina = 0;

  constructor(
    private resenaService: ResenaService,
    private productoService: ProductoService,
    protected authService: AuthService,
    protected router: Router
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  /** Primera página y permiso juntos. */
  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.pagina = 0;
    forkJoin([
      this.resenaService.listar(this.idProducto(), 0, TAMANIO_PAGINA),
      this.resenaService.permiso(this.idProducto())
    ]).subscribe({
      next: ([pagina, permiso]) => {
        this.cargando.set(false);
        this.resenas.set(pagina.content);
        this.hayMas.set(pagina.page < pagina.totalPages - 1);
        this.permiso.set(permiso);
      },
      error: (e: unknown) => {
        this.cargando.set(false);
        this.error.set(
          e instanceof HttpErrorResponse
            ? traducirErrorResena(e)
            : { titulo: 'No pudimos cargar las opiniones', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
      }
    });
  }

  verMas(): void {
    this.cargandoMas.set(true);
    this.resenaService.listar(this.idProducto(), this.pagina + 1, TAMANIO_PAGINA).subscribe({
      next: (pagina) => {
        this.cargandoMas.set(false);
        this.pagina = pagina.page;
        this.resenas.update((actuales) => [...actuales, ...pagina.content]);
        this.hayMas.set(pagina.page < pagina.totalPages - 1);
      },
      error: () => this.cargandoMas.set(false)
    });
  }

  elegirEstrellas(cantidad: number): void {
    this.estrellasElegidas.set(cantidad);
    this.errorPublicar.set(null);
  }

  errorComentario(): string | null {
    return mensajeDeError(this.comentario, 'resenaComentario');
  }

  publicar(): void {
    this.intentoPublicar.set(true);
    this.errorPublicar.set(null);
    if (!this.estrellasElegidas() || this.comentario.invalid) {
      return;
    }
    const texto = this.comentario.value.trim();
    this.publicando.set(true);
    this.resenaService
      .crear(this.idProducto(), { estrellas: this.estrellasElegidas(), comentario: texto || null })
      .subscribe({
        next: () => {
          this.publicando.set(false);
          this.recienPublicada.set(true);
          this.actualizarTodo();
        },
        error: (e: unknown) => {
          this.publicando.set(false);
          this.errorPublicar.set(
            e instanceof HttpErrorResponse
              ? traducirErrorResena(e)
              : { titulo: 'No pudimos publicar tu opinión', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
          );
        }
      });
  }

  pedirBorrado(resena: Resena): void {
    this.errorBorrar.set(null);
    this.resenaABorrar.set(resena);
  }

  cerrarBorrado(): void {
    if (!this.borrando()) {
      this.resenaABorrar.set(null);
    }
  }

  confirmarBorrado(): void {
    const resena = this.resenaABorrar();
    if (!resena) {
      return;
    }
    this.borrando.set(true);
    this.resenaService.eliminar(this.idProducto(), resena.id).subscribe({
      next: () => {
        this.borrando.set(false);
        this.resenaABorrar.set(null);
        this.actualizarTodo();
      },
      error: (e: unknown) => {
        this.borrando.set(false);
        this.errorBorrar.set(
          e instanceof HttpErrorResponse
            ? traducirErrorResena(e)
            : { titulo: 'No pudimos eliminar la opinión', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
      }
    });
  }

  /** Después de opinar o borrar: lista, permiso y promedio del producto, desde el back. */
  private actualizarTodo(): void {
    this.cargar();
    this.productoService.obtener(this.idProducto()).subscribe({
      next: (producto) => {
        const resumen = { promedio: producto.promedioResenas, cantidad: producto.cantidadResenas };
        this.resumen.set(resumen);
        this.resumenCambio.emit(resumen);
      }
    });
  }
}
