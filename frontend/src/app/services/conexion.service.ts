import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../environments/environment';

/** Cuánto se ve el aviso verde de "Volvió la conexión". */
const DURACION_RECUPERADA_MS = 4000;

/**
 * Estado de la conexión con el back, compartido por toda la app.
 * Lo actualiza conexionInterceptor con cada respuesta y lo muestra AvisoConexion,
 * así el "no pudimos conectarnos" aparece una sola vez y no en cada pantalla.
 */
@Injectable({
  providedIn: 'root'
})
export class ConexionService {
  private readonly API_URL = environment.apiUrl;

  /** null: todo bien. 'red': el back no respondió. 'servidor': respondió con 5xx. */
  readonly problema = signal<'red' | 'servidor' | null>(null);
  readonly reintentando = signal(false);
  /** Se acaba de recuperar la conexión: se muestra unos segundos y se va solo. */
  readonly recuperada = signal(false);

  private temporizador: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  reportarFallo(tipo: 'red' | 'servidor'): void {
    this.problema.set(tipo);
    this.recuperada.set(false);
  }

  reportarExito(): void {
    if (!this.problema()) {
      return;
    }
    this.problema.set(null);
    this.recuperada.set(true);
    if (this.temporizador) {
      clearTimeout(this.temporizador);
    }
    this.temporizador = setTimeout(() => this.recuperada.set(false), DURACION_RECUPERADA_MS);
  }

  /** Prueba el back con un pedido liviano y, si responde, vuelve a cargar la pantalla actual. */
  reintentar(): void {
    if (this.reintentando()) {
      return;
    }
    this.reintentando.set(true);
    this.http.get(`${this.API_URL}/categorias`).subscribe({
      next: () => {
        this.reintentando.set(false);
        this.recargarPantalla();
      },
      // El interceptor ya dejó el problema marcado: la franja sigue visible.
      error: () => this.reintentando.set(false)
    });
  }

  cerrarRecuperada(): void {
    this.recuperada.set(false);
  }

  /** Las pantallas cargan sus datos al crearse: se sale y se vuelve a la misma URL para recrearlas. */
  private recargarPantalla(): void {
    const url = this.router.url;
    this.router.navigateByUrl('/', { skipLocationChange: true }).then(() => this.router.navigateByUrl(url));
  }
}
