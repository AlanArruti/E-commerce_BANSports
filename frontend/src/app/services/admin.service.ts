import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ResumenAdmin } from '../models/resumen-admin';
import { VarianteStockBajo } from '../models/variante-stock-bajo';

/** Datos del panel de administración (resumen y stock bajo). */
@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly API_URL = environment.apiUrl;

  constructor(private http: HttpClient) {}

  resumen(): Observable<ResumenAdmin> {
    return this.http.get<ResumenAdmin>(`${this.API_URL}/admin/resumen`);
  }

  variantesStockBajo(): Observable<VarianteStockBajo[]> {
    return this.http.get<VarianteStockBajo[]>(`${this.API_URL}/admin/variantes/stock-bajo`);
  }
}
