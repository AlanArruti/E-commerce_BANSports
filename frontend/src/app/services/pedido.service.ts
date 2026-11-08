import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Pagina } from '../models/pagina';
import { EnvioRequest, EstadoManual, EstadoPedido, Pedido } from '../models/pedido';

/** Gestión de pedidos del panel (SCRUM-138). Todos los endpoints de escritura son solo ADMIN. */
@Injectable({
  providedIn: 'root'
})
export class PedidoService {
  private readonly API_URL = `${environment.apiUrl}/pedido`;
  private readonly http = inject(HttpClient);

  /** Página de pedidos, del más nuevo al más viejo. Sin estado trae todos. */
  listar(estado: EstadoPedido | null, pagina: number, tamanio: number): Observable<Pagina<Pedido>> {
    let params = new HttpParams().set('page', pagina).set('size', tamanio);
    if (estado) {
      params = params.set('estado', estado);
    }
    return this.http.get<Pagina<Pedido>>(this.API_URL, { params });
  }

  obtener(id: number): Observable<Pedido> {
    return this.http.get<Pedido>(`${this.API_URL}/${id}`);
  }

  cambiarEstado(id: number, estado: EstadoManual): Observable<Pedido> {
    return this.http.patch<Pedido>(`${this.API_URL}/${id}/estado`, { estado });
  }

  /** Si ya estaba cobrado, el back devuelve el stock. */
  cancelar(id: number): Observable<void> {
    return this.http.patch<void>(`${this.API_URL}/${id}/cancelar`, {});
  }

  /** Crea el envío a la dirección del pedido y lo pasa a DESPACHADO. Solo para pedidos PAGADO. */
  despachar(id: number, fechas: EnvioRequest): Observable<unknown> {
    return this.http.post(`${this.API_URL}/${id}/envio`, fechas);
  }
}
