import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
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

  listarPedidos(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(`${this.API_URL}/cliente/${this.idCliente()}/pedido`);
  }

  private idCliente(): number {
    const id = this.authService.usuario()?.id;
    if (id === undefined) {
      throw new Error('No hay un cliente con sesión iniciada.');
    }
    return id;
  }
}
