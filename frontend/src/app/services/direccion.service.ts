import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Direccion } from '../models/direccion';
import { DireccionRequest } from '../models/direccion-request';
import { AuthService } from './auth.service';

/** Direcciones de entrega del cliente logueado. */
@Injectable({
  providedIn: 'root'
})
export class DireccionService {
  private readonly API_URL = environment.apiUrl;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  listar(): Observable<Direccion[]> {
    return this.http.get<Direccion[]>(this.url());
  }

  agregar(direccion: DireccionRequest): Observable<Direccion> {
    return this.http.post<Direccion>(this.url(), direccion);
  }

  private url(): string {
    return `${this.API_URL}/cliente/${this.authService.cliente()?.id}/direccion`;
  }
}
