import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { Direccion } from '../models/direccion';
import { DireccionRequest } from '../models/direccion-request';
import { AuthService } from './auth.service';

/**
 * Direcciones de entrega del cliente logueado.
 * Cada cambio se refleja en `authService.cliente`, que es de donde las lee el perfil:
 * así una dirección cargada en el checkout aparece en el perfil y una borrada desaparece del checkout.
 */
@Injectable({
  providedIn: 'root'
})
export class DireccionService {
  private readonly API_URL = environment.apiUrl;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  listar(): Observable<Direccion[]> {
    return this.http
      .get<Direccion[]>(this.url())
      .pipe(tap((direcciones) => this.actualizarSesion(() => direcciones)));
  }

  agregar(direccion: DireccionRequest): Observable<Direccion> {
    return this.http
      .post<Direccion>(this.url(), direccion)
      .pipe(tap((nueva) => this.actualizarSesion((direcciones) => [...direcciones, nueva])));
  }

  /** Baja lógica en el back: los pedidos viejos la siguen mostrando. */
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url()}/${id}`).pipe(tap(() => this.quitarDeLaSesion(id)));
  }

  /** También para el 404 de una que ya estaba dada de baja. */
  quitarDeLaSesion(id: number): void {
    this.actualizarSesion((direcciones) => direcciones.filter((direccion) => direccion.id !== id));
  }

  private actualizarSesion(cambio: (direcciones: Direccion[]) => Direccion[]): void {
    this.authService.cliente.update((cliente) =>
      cliente ? { ...cliente, direcciones: cambio(cliente.direcciones ?? []) } : cliente
    );
  }

  private url(): string {
    return `${this.API_URL}/cliente/${this.authService.cliente()?.id}/direccion`;
  }
}
