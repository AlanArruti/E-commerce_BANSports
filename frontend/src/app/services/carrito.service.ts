import { Injectable, computed, effect, signal, untracked } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { Carrito } from '../models/carrito';
import { ItemCarritoRequest } from '../models/item-carrito-request';
import { Pedido } from '../models/pedido';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class CarritoService {
  private readonly API_URL = environment.apiUrl;

  /** Carrito del cliente logueado. null si no hay sesión o todavía no se cargó. */
  readonly carrito = signal<Carrito | null>(null);

  /** Unidades en el carrito (no ítems): es lo que muestra el contador del header. */
  readonly cantidadTotal = computed(
    () => this.carrito()?.items.reduce((total, item) => total + item.cantidad, 0) ?? 0
  );

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {
    // El carrito sigue a la sesión: se carga cuando el back confirma al cliente y se borra al salir.
    effect(() => {
      const cliente = this.authService.cliente();
      untracked(() => {
        if (cliente) {
          // Si falla, el contador queda vacío; la página del carrito muestra el error al entrar.
          this.cargar().subscribe({ error: () => this.carrito.set(null) });
        } else {
          this.carrito.set(null);
        }
      });
    });
  }

  cargar(): Observable<Carrito> {
    return this.http.get<Carrito>(this.url()).pipe(tap((carrito) => this.carrito.set(carrito)));
  }

  /** Agrega un producto. Si ya estaba, el back suma la cantidad y valida que no supere el stock. */
  agregar(item: ItemCarritoRequest): Observable<Carrito> {
    return this.http.post<Carrito>(`${this.url()}/items`, item).pipe(tap((carrito) => this.carrito.set(carrito)));
  }

  actualizarCantidad(idItem: number, cantidad: number): Observable<Carrito> {
    return this.http
      .put<Carrito>(`${this.url()}/items/${idItem}`, { cantidad })
      .pipe(tap((carrito) => this.carrito.set(carrito)));
  }

  eliminar(idItem: number): Observable<Carrito> {
    return this.http
      .delete<Carrito>(`${this.url()}/items/${idItem}`)
      .pipe(tap((carrito) => this.carrito.set(carrito)));
  }

  vaciar(): Observable<void> {
    return this.http.delete<void>(this.url()).pipe(
      tap(() => this.carrito.update((carrito) => (carrito ? { ...carrito, items: [], total: 0 } : carrito)))
    );
  }

  /**
   * Convierte el carrito en un pedido (queda en PENDIENTE_PAGO). El back valida el stock
   * y que la dirección sea del cliente, y vacía el carrito.
   */
  checkout(idDireccion: number): Observable<Pedido> {
    return this.http
      .post<Pedido>(`${this.url()}/checkout`, { idDireccion })
      .pipe(tap(() => this.carrito.update((carrito) => (carrito ? { ...carrito, items: [], total: 0 } : carrito))));
  }

  /** Cuántas unidades de esa variante ya hay en el carrito (para no ofrecer más que el stock). */
  cantidadDeVariante(idVariante: number): number {
    return this.carrito()?.items.find((item) => item.variante?.id === idVariante)?.cantidad ?? 0;
  }

  private url(): string {
    return `${this.API_URL}/cliente/${this.authService.cliente()?.id}/carrito`;
  }
}
