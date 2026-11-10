import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ImagenProducto } from '../models/imagen-producto';

/**
 * Imagen de un producto (solo ADMIN para subir y borrar).
 * El back guarda una sola imagen por producto y una por variante: subir una nueva reemplaza la anterior.
 * Con idVariante se trabaja sobre la foto de esa variante; sin él, sobre la del producto.
 */
@Injectable({
  providedIn: 'root'
})
export class ImagenProductoService {
  private readonly API_URL = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /** POST /producto/{id}/imagen como multipart/form-data, en el campo "archivo". */
  subir(idProducto: number, archivo: File, idVariante: number | null = null): Observable<ImagenProducto> {
    const datos = new FormData();
    datos.append('archivo', archivo, archivo.name);
    // Sin Content-Type a mano: el navegador lo arma con el boundary del multipart.
    return this.http.post<ImagenProducto>(this.url(idProducto, idVariante), datos);
  }

  /** DELETE /producto/{id}/imagen. */
  eliminar(idProducto: number, idVariante: number | null = null): Observable<void> {
    return this.http.delete<void>(this.url(idProducto, idVariante));
  }

  private url(idProducto: number, idVariante: number | null): string {
    const base = `${this.API_URL}/producto/${idProducto}`;
    return idVariante == null ? `${base}/imagen` : `${base}/variante/${idVariante}/imagen`;
  }
}
