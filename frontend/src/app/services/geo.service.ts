import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Localidad } from '../models/geo';

/** Búsqueda de localidades de Argentina (Georef), a través de nuestro back. */
@Injectable({
  providedIn: 'root'
})
export class GeoService {
  private readonly API_URL = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /** GET /geo/localidades. Con menos de 3 caracteres el back devuelve lista vacía. */
  localidades(texto: string): Observable<Localidad[]> {
    const params = new HttpParams().set('texto', texto);
    return this.http.get<Localidad[]>(`${this.API_URL}/geo/localidades`, { params });
  }
}
