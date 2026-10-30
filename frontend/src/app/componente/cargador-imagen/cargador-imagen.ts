import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  effect,
  input,
  linkedSignal,
  output,
  signal,
  viewChild
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ImagenProductoService } from '../../services/imagen-producto.service';
import { ErrorTraducido } from '../../validaciones/errores-api';
import {
  EXTENSIONES_IMAGEN,
  formatearTamanio,
  traducirErrorImagen,
  validarImagen
} from '../../validaciones/imagen-producto';

/**
 * Sube, reemplaza y elimina la imagen de un producto.
 * Uso: <app-cargador-imagen [productoId]="id" [imagenUrl]="producto.imagenUrl" (imagenCambiada)="..." />
 */
@Component({
  selector: 'app-cargador-imagen',
  templateUrl: './cargador-imagen.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './cargador-imagen.css',
  host: { '(document:keydown.escape)': 'cerrarConfirmacion()' }
})
export class CargadorImagen {
  readonly productoId = input.required<number>();
  /** Imagen que ya tiene el producto (imagenUrl del ProductoResponse), o null. */
  readonly imagenUrl = input<string | null>(null);
  /** Avisa la nueva URL después de subir, o null después de eliminar. */
  readonly imagenCambiada = output<string | null>();

  protected readonly extensiones = EXTENSIONES_IMAGEN;
  protected readonly formatearTamanio = formatearTamanio;

  protected readonly imagenActual = linkedSignal(() => this.imagenUrl());
  protected readonly imagenActualFallo = signal(false);

  protected readonly archivo = signal<File | null>(null);
  protected readonly vistaPrevia = signal<string | null>(null);
  protected readonly arrastrando = signal(false);

  protected readonly errorArchivo = signal<string | null>(null);
  protected readonly errorServidor = signal<ErrorTraducido | null>(null);
  protected readonly aviso = signal<string | null>(null);

  protected readonly subiendo = signal(false);
  protected readonly eliminando = signal(false);
  protected readonly confirmando = signal(false);

  private readonly botonCancelarEliminar = viewChild<ElementRef<HTMLButtonElement>>('botonCancelarEliminar');

  constructor(
    private imagenService: ImagenProductoService,
    destroyRef: DestroyRef
  ) {
    // Al abrir la confirmación, el foco va a "Cancelar" (la opción segura).
    effect(() => this.botonCancelarEliminar()?.nativeElement.focus());
    destroyRef.onDestroy(() => this.liberarVistaPrevia());
  }

  // ── Elegir archivo ─────────────────────────────────────

  alElegir(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    const elegido = campo.files?.[0];
    if (elegido) {
      this.tomarArchivo(elegido);
    }
    // Se limpia para que elegir el mismo archivo otra vez vuelva a disparar el cambio.
    campo.value = '';
  }

  alArrastrarEncima(evento: DragEvent): void {
    evento.preventDefault();
    this.arrastrando.set(true);
  }

  alSalirArrastre(evento: DragEvent): void {
    // dragleave también salta al pasar sobre un hijo de la zona: solo cuenta si salió de verdad.
    const zona = evento.currentTarget as HTMLElement;
    if (!zona.contains(evento.relatedTarget as Node | null)) {
      this.arrastrando.set(false);
    }
  }

  alSoltar(evento: DragEvent): void {
    evento.preventDefault();
    this.arrastrando.set(false);
    const archivos = evento.dataTransfer?.files;
    if (!archivos?.length) {
      return;
    }
    if (archivos.length > 1) {
      this.errorArchivo.set('Soltá una sola imagen: cada producto tiene una.');
      return;
    }
    this.tomarArchivo(archivos[0]);
  }

  descartar(): void {
    this.liberarVistaPrevia();
    this.archivo.set(null);
    this.errorArchivo.set(null);
    this.errorServidor.set(null);
  }

  // ── Subir y eliminar ───────────────────────────────────

  subir(): void {
    const archivo = this.archivo();
    if (!archivo || this.subiendo()) {
      return;
    }
    this.subiendo.set(true);
    this.errorServidor.set(null);
    const reemplaza = this.imagenActual() !== null;

    this.imagenService.subir(this.productoId(), archivo).subscribe({
      next: (imagen) => {
        this.subiendo.set(false);
        // El GET de la imagen se cachea 1 hora: el parámetro fuerza a pedir la nueva.
        const url = `${imagen.url}?v=${Date.now()}`;
        this.imagenActual.set(url);
        this.imagenActualFallo.set(false);
        this.descartar();
        this.aviso.set(reemplaza ? 'Listo, reemplazaste la imagen.' : 'Listo, el producto ya tiene imagen.');
        this.imagenCambiada.emit(url);
      },
      error: (e: unknown) => {
        this.subiendo.set(false);
        this.errorServidor.set(this.traducir(e, 'subir'));
      }
    });
  }

  abrirConfirmacion(): void {
    this.errorServidor.set(null);
    this.aviso.set(null);
    this.confirmando.set(true);
  }

  cerrarConfirmacion(): void {
    if (!this.eliminando()) {
      this.confirmando.set(false);
    }
  }

  eliminar(): void {
    this.eliminando.set(true);
    this.imagenService.eliminar(this.productoId()).subscribe({
      next: () => {
        this.eliminando.set(false);
        this.confirmando.set(false);
        this.imagenActual.set(null);
        this.aviso.set('Eliminaste la imagen del producto.');
        this.imagenCambiada.emit(null);
      },
      error: (e: unknown) => {
        this.eliminando.set(false);
        this.confirmando.set(false);
        this.errorServidor.set(this.traducir(e, 'eliminar'));
      }
    });
  }

  // ── Internos ───────────────────────────────────────────

  private tomarArchivo(archivo: File): void {
    this.aviso.set(null);
    this.errorServidor.set(null);
    const error = validarImagen(archivo);
    if (error) {
      this.descartar();
      this.errorArchivo.set(error);
      return;
    }
    this.liberarVistaPrevia();
    this.errorArchivo.set(null);
    this.archivo.set(archivo);
    this.vistaPrevia.set(URL.createObjectURL(archivo));
  }

  private liberarVistaPrevia(): void {
    const url = this.vistaPrevia();
    if (url) {
      URL.revokeObjectURL(url);
      this.vistaPrevia.set(null);
    }
  }

  private traducir(e: unknown, accion: 'subir' | 'eliminar'): ErrorTraducido {
    if (e instanceof HttpErrorResponse) {
      return traducirErrorImagen(e, accion);
    }
    return {
      titulo: accion === 'subir' ? 'No pudimos subir la imagen' : 'No pudimos eliminar la imagen',
      detalle: 'Ocurrió un error inesperado. Intentá de nuevo.'
    };
  }
}
