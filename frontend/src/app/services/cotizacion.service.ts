import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CotizacionEnvio } from '../models/cotizacion-envio';

/** Cotización de envío por dirección (Correo Argentino o tabla propia de respaldo). */
@Injectable({
  providedIn: 'root'
})
export class CotizacionService {
  private readonly API_URL = environment.apiUrl;

  constructor(private http: HttpClient) {}

  cotizar(idDireccion: number): Observable<CotizacionEnvio[]> {
    return this.http.get<CotizacionEnvio[]>(`${this.API_URL}/cotizacion/envio/${idDireccion}`);
  }
}
