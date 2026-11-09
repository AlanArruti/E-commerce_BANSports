import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Pagina } from '../models/pagina';
import { EstadoPedido, Pedido } from '../models/pedido';

/** Pedidos del cliente logueado o, para ADMIN, de todos (GET /pedido). */
@Injectable({
  providedIn: 'root'
})
export class PedidoService {
  private readonly API_URL = environment.apiUrl;

  constructor(private http: HttpClient) {}

  listar(filtros: { estado?: EstadoPedido | null; page: number; size: number }): Observable<Pagina<Pedido>> {
    let params = new HttpParams().set('page', filtros.page).set('size', filtros.size);
    if (filtros.estado) {
      params = params.set('estado', filtros.estado);
    }
    return this.http.get<Pagina<Pedido>>(`${this.API_URL}/pedido`, { params });
  }
}
