import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Pagina } from '../models/pagina';
import { PermisoResena, Resena, ResenaRequest } from '../models/resena';

/** Reseñas de productos: leerlas es público; opinar es del cliente que compró; borrar, del admin. */
@Injectable({
  providedIn: 'root'
})
export class ResenaService {
  private readonly API_URL = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /** De la más nueva a la más vieja. La página empieza en 0. */
  listar(idProducto: number, pagina: number, tamanio: number): Observable<Pagina<Resena>> {
    const params = new HttpParams().set('page', pagina).set('size', tamanio);
    return this.http.get<Pagina<Resena>>(this.url(idProducto), { params });
  }

  /** Con sesión, el interceptor manda el token y el back sabe si este cliente puede opinar. */
  permiso(idProducto: number): Observable<PermisoResena> {
    return this.http.get<PermisoResena>(`${this.url(idProducto)}/permiso`);
  }

  /** 403 si no compró el producto, 409 si ya opinó. */
  crear(idProducto: number, resena: ResenaRequest): Observable<Resena> {
    return this.http.post<Resena>(this.url(idProducto), resena);
  }

  /** Solo ADMIN (moderación). */
  eliminar(idProducto: number, idResena: number): Observable<void> {
    return this.http.delete<void>(`${this.url(idProducto)}/${idResena}`);
  }

  private url(idProducto: number): string {
    return `${this.API_URL}/producto/${idProducto}/resenas`;
  }
}
