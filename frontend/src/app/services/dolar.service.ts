import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { ActualizacionDolar, AjustePrecios, CotizacionDolar } from '../models/dolar';

/**
 * Cotización del dólar y ajuste de precios (RF01). La cotización es pública: la ve el
 * cliente en la franja de arriba y el admin en su panel. El historial y la actualización
 * manual son solo del admin.
 */
@Injectable({
  providedIn: 'root'
})
export class DolarService {
  private readonly API_URL = `${environment.apiUrl}/dolar`;

  /** Última cotización leída. null mientras no hay (o si el back todavía no la tiene). */
  readonly cotizacion = signal<CotizacionDolar | null>(null);

  constructor(private http: HttpClient) {}

  cargar(): Observable<CotizacionDolar> {
    return this.http.get<CotizacionDolar>(this.API_URL).pipe(tap((c) => this.cotizacion.set(c)));
  }

  /** Para la franja de la tienda: si falla, queda la última que había (o nada). */
  refrescar(): void {
    this.cargar().subscribe({ error: () => {} });
  }

  historial(): Observable<AjustePrecios[]> {
    return this.http.get<AjustePrecios[]>(`${this.API_URL}/historial`);
  }

  /** Consulta el dólar en el momento. Con forzar=true ajusta aunque el salto supere el máximo. */
  actualizar(forzar = false): Observable<ActualizacionDolar> {
    return this.http
      .post<ActualizacionDolar>(`${this.API_URL}/actualizar`, null, { params: { forzar } })
      .pipe(tap((r) => this.cotizacion.set(r.cotizacion)));
  }
}
