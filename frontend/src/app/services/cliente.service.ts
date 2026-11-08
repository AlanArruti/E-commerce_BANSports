import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { CambioPasswordRequest } from '../models/cambio-password-request';
import { Cliente } from '../models/cliente';
import { ClienteUpdateRequest } from '../models/cliente-update-request';
import { Pedido } from '../models/pedido';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class ClienteService {
  private readonly API_URL = environment.apiUrl;
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  actualizarPerfil(datos: ClienteUpdateRequest): Observable<Cliente> {
    return this.http.put<Cliente>(`${this.API_URL}/cliente/${this.idCliente()}`, datos).pipe(
      tap((cliente) => this.authService.cliente.set(cliente))
    );
  }

  cambiarPassword(datos: CambioPasswordRequest): Observable<void> {
    return this.http.patch<void>(`${this.API_URL}/cliente/${this.idCliente()}/password`, datos);
  }

  /** Historial del cliente. El back no los ordena: van del más nuevo al más viejo. */
  listarPedidos(): Observable<Pedido[]> {
    return this.http
      .get<Pedido[]>(`${this.API_URL}/cliente/${this.idCliente()}/pedido`)
      .pipe(map((pedidos) => [...pedidos].sort((a, b) => b.fechaCreacion.localeCompare(a.fechaCreacion))));
  }

  /** Un pedido propio. Si es de otro cliente el back responde 403, y si no existe, 404. */
  obtenerPedido(id: number): Observable<Pedido> {
    return this.http.get<Pedido>(`${this.API_URL}/pedido/${id}`);
  }

  private idCliente(): number {
    const id = this.authService.usuario()?.id;
    if (id === undefined) {
      throw new Error('No hay un cliente con sesión iniciada.');
    }
    return id;
  }
}
