import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { Pagina } from '../models/pagina';
import { Envio, EnvioRequest, EstadoEnvio, EstadoManual, EstadoPedido, Pedido } from '../models/pedido';

/**
 * Pedidos del cliente logueado o, para ADMIN, de todos (GET /pedido), y la gestión de
 * pedidos y envíos del panel (SCRUM-138 y 139). Los endpoints de escritura son solo ADMIN.
 */
@Injectable({
  providedIn: 'root'
})
export class PedidoService {
  private readonly API_URL = `${environment.apiUrl}/pedido`;

  constructor(private http: HttpClient) {}

  /** Página de pedidos, del más nuevo al más viejo. Sin estado trae todos. */
  listar(filtros: { estado?: EstadoPedido | null; page: number; size: number }): Observable<Pagina<Pedido>> {
    let params = new HttpParams().set('page', filtros.page).set('size', filtros.size);
    if (filtros.estado) {
      params = params.set('estado', filtros.estado);
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
  despachar(id: number, envio: EnvioRequest): Observable<Envio> {
    return this.http.post<Envio>(`${this.API_URL}/${id}/envio`, envio);
  }

  /** DESPACHADO → EN_CAMINO → ENTREGADO. Al entregar, el back también marca el pedido ENTREGADO. */
  avanzarEnvio(idPedido: number, estado: EstadoEnvio): Observable<Envio> {
    return this.http.patch<Envio>(`${this.API_URL}/${idPedido}/envio/estado`, { estado });
  }

  /** Todos los envíos (GET /envio, sin paginar), del que llega antes al que llega después. */
  listarEnvios(estado: EstadoEnvio | null): Observable<Envio[]> {
    const params = estado ? new HttpParams().set('estado', estado) : undefined;
    return this.http
      .get<Envio[]>(`${environment.apiUrl}/envio`, { params })
      .pipe(
        map((envios) =>
          [...envios].sort(
            (a, b) => (a.fechaLlegada ?? '').localeCompare(b.fechaLlegada ?? '') || b.idPedido - a.idPedido
          )
        )
      );
  }
}
