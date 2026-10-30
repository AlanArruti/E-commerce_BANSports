import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Lugar, SugerenciaDireccion } from '../models/geo';

/**
 * Búsqueda de direcciones con Google Maps, a través de nuestro back (la key no sale del servidor).
 * "sesion" agrupa las sugerencias y el detalle de una misma búsqueda, para que Google la cobre como una.
 */
@Injectable({
  providedIn: 'root'
})
export class GeoService {
  private readonly API_URL = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /** GET /geo/sugerencias. Con menos de 3 caracteres el back devuelve lista vacía. */
  sugerencias(texto: string, sesion: string): Observable<SugerenciaDireccion[]> {
    const params = new HttpParams().set('texto', texto).set('sesion', sesion);
    return this.http.get<SugerenciaDireccion[]>(`${this.API_URL}/geo/sugerencias`, { params });
  }

  /** GET /geo/lugares/{placeId}: la dirección elegida, desarmada en partes. */
  lugar(placeId: string, sesion: string): Observable<Lugar> {
    const params = new HttpParams().set('sesion', sesion);
    return this.http.get<Lugar>(`${this.API_URL}/geo/lugares/${encodeURIComponent(placeId)}`, { params });
  }
}
