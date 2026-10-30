import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ImagenProducto } from '../models/imagen-producto';

/**
 * Imagen de un producto (solo ADMIN para subir y borrar).
 * El back guarda una sola imagen por producto: subir una nueva reemplaza la anterior.
 */
@Injectable({
  providedIn: 'root'
})
export class ImagenProductoService {
  private readonly API_URL = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /** POST /producto/{id}/imagen como multipart/form-data, en el campo "archivo". */
  subir(idProducto: number, archivo: File): Observable<ImagenProducto> {
    const datos = new FormData();
    datos.append('archivo', archivo, archivo.name);
    // Sin Content-Type a mano: el navegador lo arma con el boundary del multipart.
    return this.http.post<ImagenProducto>(`${this.API_URL}/producto/${idProducto}/imagen`, datos);
  }

  /** DELETE /producto/{id}/imagen. */
  eliminar(idProducto: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/producto/${idProducto}/imagen`);
  }
}
