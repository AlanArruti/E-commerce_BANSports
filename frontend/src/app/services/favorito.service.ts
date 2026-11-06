import { Injectable, computed, effect, signal, untracked } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { Favorito } from '../models/favorito';
import { Producto } from '../models/producto';
import { ErrorTraducido, traducirErrorFavoritos } from '../validaciones/errores-api';
import { AuthService } from './auth.service';

/** Producto que se quiso guardar sin sesión: se guarda solo al volver del login. */
const CLAVE_PENDIENTE = 'ban_favorito_pendiente';

@Injectable({
  providedIn: 'root'
})
export class FavoritoService {
  private readonly API_URL = environment.apiUrl;

  /** Favoritos del cliente logueado. null si no hay sesión o todavía no se cargaron. */
  readonly favoritos = signal<Favorito[] | null>(null);

  /** Ids de los productos guardados, para pintar el corazón sin recorrer la lista. */
  readonly ids = computed(() => new Set(this.favoritos()?.map((favorito) => favorito.producto.id) ?? []));

  readonly cantidad = computed(() => this.favoritos()?.length ?? 0);

  /** Último producto quitado con el corazón: la página de favoritos ofrece "Deshacer". */
  readonly ultimoQuitado = signal<Producto | null>(null);

  /** Falló el último click en un corazón (el corazón ya volvió a como estaba). */
  readonly errorAccion = signal<ErrorTraducido | null>(null);

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private router: Router
  ) {
    // Igual que el carrito: los favoritos siguen a la sesión.
    effect(() => {
      const cliente = this.authService.cliente();
      untracked(() => {
        if (cliente) {
          this.cargar().subscribe({
            next: () => this.guardarPendiente(),
            error: () => this.favoritos.set(null)
          });
        } else {
          this.favoritos.set(null);
          this.ultimoQuitado.set(null);
        }
      });
    });
  }

  cargar(): Observable<Favorito[]> {
    return this.http.get<Favorito[]>(this.url()).pipe(tap((favoritos) => this.favoritos.set(favoritos)));
  }

  esFavorito(idProducto: number): boolean {
    return this.ids().has(idProducto);
  }

  /**
   * Agrega o quita el producto. El corazón cambia al instante y vuelve atrás si el back falla.
   * Sin sesión, manda al login y guarda el producto para agregarlo al volver.
   */
  alternar(producto: Producto): void {
    if (this.authService.esAdmin()) {
      return;
    }
    if (!this.authService.estaLogueado()) {
      this.recordarPendiente(producto.id);
      this.router.navigate(['/login'], { queryParams: { redirect: this.router.url, motivo: 'favoritos' } });
      return;
    }
    if (!this.authService.cliente()) {
      // Todavía no llegó la ficha del cliente (recién cargó la página): no hay a qué URL pegarle.
      return;
    }
    this.errorAccion.set(null);
    if (this.esFavorito(producto.id)) {
      this.quitar(producto);
    } else {
      this.agregar(producto);
    }
  }

  private agregar(producto: Producto): void {
    const nuevo: Favorito = { producto, fechaAgregado: new Date().toISOString() };
    this.favoritos.update((favoritos) => [nuevo, ...(favoritos ?? [])]);
    if (this.ultimoQuitado()?.id === producto.id) {
      this.ultimoQuitado.set(null);
    }
    this.http.post<Favorito>(`${this.url()}/${producto.id}`, null).subscribe({
      error: (e: unknown) => {
        this.favoritos.update((favoritos) => favoritos?.filter((favorito) => favorito !== nuevo) ?? null);
        this.errorAccion.set(this.traducir(e));
      }
    });
  }

  private quitar(producto: Producto): void {
    const anteriores = this.favoritos();
    this.favoritos.update((favoritos) => favoritos?.filter((favorito) => favorito.producto.id !== producto.id) ?? null);
    this.ultimoQuitado.set(producto);
    this.http.delete<void>(`${this.url()}/${producto.id}`).subscribe({
      error: (e: unknown) => {
        this.favoritos.set(anteriores);
        this.ultimoQuitado.set(null);
        this.errorAccion.set(this.traducir(e));
      }
    });
  }

  /** Guarda el producto que se eligió antes de ir al login. Si ya no existe, se descarta. */
  private guardarPendiente(): void {
    const id = this.leerPendiente();
    if (id === null) {
      return;
    }
    this.olvidarPendiente();
    if (this.esFavorito(id)) {
      return;
    }
    this.http.post<Favorito>(`${this.url()}/${id}`, null).subscribe({
      next: (favorito) => this.favoritos.update((favoritos) => [favorito, ...(favoritos ?? [])]),
      error: () => {}
    });
  }

  // sessionStorage puede fallar (modo privado, almacenamiento bloqueado): en ese caso solo se pierde el pendiente.
  private recordarPendiente(idProducto: number): void {
    try {
      sessionStorage.setItem(CLAVE_PENDIENTE, String(idProducto));
    } catch {
      // Sin almacenamiento, el cliente tendrá que tocar el corazón de nuevo al volver.
    }
  }

  private leerPendiente(): number | null {
    try {
      const valor = Number(sessionStorage.getItem(CLAVE_PENDIENTE));
      return Number.isInteger(valor) && valor > 0 ? valor : null;
    } catch {
      return null;
    }
  }

  private olvidarPendiente(): void {
    try {
      sessionStorage.removeItem(CLAVE_PENDIENTE);
    } catch {
      // Nada que limpiar.
    }
  }

  private traducir(e: unknown): ErrorTraducido {
    return e instanceof HttpErrorResponse
      ? traducirErrorFavoritos(e)
      : { titulo: 'No pudimos actualizar tus favoritos', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' };
  }

  private url(): string {
    return `${this.API_URL}/cliente/${this.authService.cliente()?.id}/favoritos`;
  }
}
