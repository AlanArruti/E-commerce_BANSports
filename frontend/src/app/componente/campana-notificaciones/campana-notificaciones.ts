import { ChangeDetectionStrategy, Component, ElementRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { Notificacion, enlaceDe } from '../../models/notificacion';
import { NotificacionService } from '../../services/notificacion.service';

/**
 * Campanita del header (RF02): contador de no leídas y, al tocarla, el panel con las últimas
 * notificaciones (pedidos y ofertas). Tocar una la marca como leída y lleva al pedido o al producto.
 */
@Component({
  selector: 'app-campana-notificaciones',
  imports: [DatePipe],
  templateUrl: './campana-notificaciones.html',
  styleUrl: './campana-notificaciones.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:click)': 'clickAfuera($event)',
    '(document:keydown.escape)': 'cerrar()'
  }
})
export class CampanaNotificaciones {
  protected readonly notificacionService = inject(NotificacionService);
  private readonly router = inject(Router);
  private readonly elemento = inject(ElementRef<HTMLElement>);

  protected readonly abierto = signal(false);
  protected readonly cargando = signal(false);
  protected readonly error = signal(false);

  alternar(): void {
    if (this.abierto()) {
      this.cerrar();
      return;
    }
    this.abierto.set(true);
    this.cargar();
  }

  cerrar(): void {
    this.abierto.set(false);
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.notificacionService.cargar().subscribe({
      next: () => {
        this.cargando.set(false);
        // Puede haber llegado algo desde la última consulta del contador.
        this.notificacionService.actualizarContador();
      },
      error: () => {
        this.cargando.set(false);
        this.error.set(true);
      }
    });
  }

  abrir(notificacion: Notificacion): void {
    this.notificacionService.marcarLeida(notificacion);
    const enlace = enlaceDe(notificacion);
    this.cerrar();
    if (enlace) {
      this.router.navigate(enlace);
    }
  }

  protected clickAfuera(evento: MouseEvent): void {
    if (this.abierto() && !this.elemento.nativeElement.contains(evento.target as Node)) {
      this.cerrar();
    }
  }
}
