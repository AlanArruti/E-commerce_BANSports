import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Administrador, AdministradorRequest } from '../models/administrador';
import { Cliente } from '../models/cliente';
import { DiasMetricas, MetricasAdmin } from '../models/metricas-admin';
import { Pagina } from '../models/pagina';
import { Pedido } from '../models/pedido';
import { ResumenAdmin } from '../models/resumen-admin';
import { VarianteStockBajo } from '../models/variante-stock-bajo';

/** Datos del panel de administración: resumen, métricas, stock, clientes y administradores. */
@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly API_URL = environment.apiUrl;

  constructor(private http: HttpClient) {}

  resumen(): Observable<ResumenAdmin> {
    return this.http.get<ResumenAdmin>(`${this.API_URL}/admin/resumen`);
  }

  metricas(dias: DiasMetricas): Observable<MetricasAdmin> {
    return this.http.get<MetricasAdmin>(`${this.API_URL}/admin/metricas`, { params: { dias } });
  }

  variantesStockBajo(): Observable<VarianteStockBajo[]> {
    return this.http.get<VarianteStockBajo[]>(`${this.API_URL}/admin/variantes/stock-bajo`);
  }

  // ── Clientes ──────────────────────────────────────────────

  /** Clientes activos, más nuevos primero. `buscar` filtra por nombre, apellido o email. */
  clientes(filtros: { buscar: string; page: number; size: number }): Observable<Pagina<Cliente>> {
    let params = new HttpParams().set('page', filtros.page).set('size', filtros.size);
    if (filtros.buscar) {
      params = params.set('buscar', filtros.buscar);
    }
    return this.http.get<Pagina<Cliente>>(`${this.API_URL}/cliente`, { params });
  }

  pedidosDeCliente(idCliente: number): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(`${this.API_URL}/cliente/${idCliente}/pedido`);
  }

  /** Baja lógica: el cliente no puede ingresar más, pero sus pedidos se conservan. */
  bajaCliente(idCliente: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/cliente/${idCliente}`);
  }

  // ── Administradores ───────────────────────────────────────

  administradores(): Observable<Administrador[]> {
    return this.http.get<Administrador[]>(`${this.API_URL}/administrador`);
  }

  crearAdministrador(request: AdministradorRequest): Observable<Administrador> {
    return this.http.post<Administrador>(`${this.API_URL}/administrador`, request);
  }

  bajaAdministrador(idAdministrador: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/administrador/${idAdministrador}`);
  }
}
