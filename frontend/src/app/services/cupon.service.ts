import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Cupon, CuponRequest, ValidacionCupon } from '../models/cupon';

/** Cupones de descuento: validar en el checkout (cliente) y administrarlos (panel). */
@Injectable({
  providedIn: 'root'
})
export class CuponService {
  private readonly API_URL = `${environment.apiUrl}/cupones`;

  constructor(private http: HttpClient) {}

  /**
   * ¿Sirve para mi carrito? El back usa el carrito del cliente logueado. Si no sirve,
   * responde 400 con el motivo (vencido, agotado, compra mínima…). No consume usos.
   */
  validar(codigo: string): Observable<ValidacionCupon> {
    return this.http.get<ValidacionCupon>(`${this.API_URL}/validar`, { params: { codigo } });
  }

  // ── Panel (solo ADMIN) ───────────────────────────────────

  listar(): Observable<Cupon[]> {
    return this.http.get<Cupon[]>(this.API_URL);
  }

  crear(cupon: CuponRequest): Observable<Cupon> {
    return this.http.post<Cupon>(this.API_URL, cupon);
  }

  actualizar(id: number, cupon: CuponRequest): Observable<Cupon> {
    return this.http.put<Cupon>(`${this.API_URL}/${id}`, cupon);
  }

  /** Baja lógica: deja de aceptarse; los pedidos que lo usaron no cambian. */
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}
