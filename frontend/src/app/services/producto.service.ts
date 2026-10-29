import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { FiltrosCatalogo, ORDENES } from '../models/filtros-catalogo';
import { Pagina } from '../models/pagina';
import { Producto } from '../models/producto';
import { Variante } from '../models/variante';

@Injectable({
  providedIn: 'root'
})
export class ProductoService {
  private readonly API_URL = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /** Catálogo paginado con búsqueda, filtros y orden (GET /producto). */
  listar(filtros: FiltrosCatalogo): Observable<Pagina<Producto>> {
    const orden = ORDENES.find((opcion) => opcion.valor === filtros.orden) ?? ORDENES[0];
    let params = new HttpParams()
      .set('sortBy', orden.sortBy)
      .set('sortDir', orden.sortDir)
      .set('page', filtros.pagina)
      .set('size', filtros.tamanio);

    if (filtros.search) {
      params = params.set('search', filtros.search);
    }
    if (filtros.categoriaId !== null) {
      params = params.set('categoriaId', filtros.categoriaId);
    }
    if (filtros.precioMin !== null) {
      params = params.set('precioMin', filtros.precioMin);
    }
    if (filtros.precioMax !== null) {
      params = params.set('precioMax', filtros.precioMax);
    }

    return this.http.get<Pagina<Producto>>(`${this.API_URL}/producto`, { params });
  }

  /** Un producto por id (GET /producto/{id}). Si no existe o está dado de baja, el back responde 404. */
  obtener(id: number): Observable<Producto> {
    return this.http.get<Producto>(`${this.API_URL}/producto/${id}`);
  }

  /** Variantes activas del producto (GET /producto/{id}/variante). */
  listarVariantes(id: number): Observable<Variante[]> {
    return this.http.get<Variante[]>(`${this.API_URL}/producto/${id}/variante`);
  }
}
