import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { DiasMetricas, MetricasAdmin } from '../models/metricas-admin';
import { ResumenAdmin } from '../models/resumen-admin';
import { VarianteStockBajo } from '../models/variante-stock-bajo';

/** Datos del panel de administración (resumen, métricas y stock bajo). */
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
}
