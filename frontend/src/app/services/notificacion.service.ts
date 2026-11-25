import { Injectable, effect, signal, untracked } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { NoLeidas, Notificacion } from '../models/notificacion';
import { AuthService } from './auth.service';

/** Cada cuánto se vuelve a consultar el contador de no leídas mientras hay sesión. */
const INTERVALO_CONSULTA_MS = 60_000;

@Injectable({
  providedIn: 'root'
})
export class NotificacionService {
  private readonly API_URL = environment.apiUrl;

  /** Notificaciones del cliente. null si no hay sesión o todavía no se abrió el panel. */
  readonly notificaciones = signal<Notificacion[] | null>(null);

  /** Contador de la campanita. */
  readonly noLeidas = signal(0);

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {
    // Como el carrito y los favoritos: siguen a la sesión. Con sesión, el contador se refresca
    // solo cada minuto, así un pedido despachado aparece sin recargar la página.
    effect((onCleanup) => {
      const cliente = this.authService.cliente();
      untracked(() => {
        this.notificaciones.set(null);
        this.noLeidas.set(0);
        if (!cliente) {
          return;
        }
        this.actualizarContador();
        const intervalo = setInterval(() => this.actualizarContador(), INTERVALO_CONSULTA_MS);
        onCleanup(() => clearInterval(intervalo));
      });
    });
  }

  cargar(): Observable<Notificacion[]> {
    return this.http.get<Notificacion[]>(this.url()).pipe(
      tap((notificaciones) => {
        this.notificaciones.set(notificaciones);
      })
    );
  }

  actualizarContador(): void {
    if (!this.authService.cliente()) {
      return;
    }
    this.http.get<NoLeidas>(`${this.url()}/no-leidas`).subscribe({
      next: ({ cantidad }) => this.noLeidas.set(cantidad),
      // Si falla, la campanita queda como estaba: no vale la pena mostrar un error por esto.
      error: () => {}
    });
  }

  /** Marca una como leída. El cambio se ve al instante y vuelve atrás si el back falla. */
  marcarLeida(notificacion: Notificacion): void {
    if (notificacion.leida) {
      return;
    }
    this.cambiarLeida(notificacion.id, true);
    this.noLeidas.update((cantidad) => Math.max(0, cantidad - 1));
    this.http.patch<Notificacion>(`${this.url()}/${notificacion.id}/leida`, null).subscribe({
      error: () => {
        this.cambiarLeida(notificacion.id, false);
        this.noLeidas.update((cantidad) => cantidad + 1);
      }
    });
  }

  marcarTodasLeidas(): void {
    const anteriores = this.notificaciones();
    const cantidadAnterior = this.noLeidas();
    this.notificaciones.update((lista) => lista?.map((n) => ({ ...n, leida: true })) ?? null);
    this.noLeidas.set(0);
    this.http.patch<NoLeidas>(`${this.url()}/leidas`, null).subscribe({
      error: () => {
        this.notificaciones.set(anteriores);
        this.noLeidas.set(cantidadAnterior);
      }
    });
  }

  private cambiarLeida(id: number, leida: boolean): void {
    this.notificaciones.update((lista) => lista?.map((n) => (n.id === id ? { ...n, leida } : n)) ?? null);
  }

  private url(): string {
    return `${this.API_URL}/cliente/${this.authService.cliente()?.id}/notificaciones`;
  }
}
