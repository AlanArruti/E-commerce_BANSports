import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Ciudad, Pais, Provincia } from '../models/geo';

/** Países, provincias y ciudades para los selects encadenados de las direcciones. */
@Injectable({
  providedIn: 'root'
})
export class GeoService {
  private readonly API_URL = environment.apiUrl;

  constructor(private http: HttpClient) {}

  paises(): Observable<Pais[]> {
    return this.http.get<Pais[]>(`${this.API_URL}/paises`);
  }

  provincias(idPais: number): Observable<Provincia[]> {
    return this.http.get<Provincia[]>(`${this.API_URL}/paises/${idPais}/provincias`);
  }

  ciudades(idProvincia: number): Observable<Ciudad[]> {
    return this.http.get<Ciudad[]>(`${this.API_URL}/provincias/${idProvincia}/ciudades`);
  }
}
