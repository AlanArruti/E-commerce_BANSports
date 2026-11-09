import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { FiltrosCatalogo, ORDEN_NUEVOS, ORDENES } from '../models/filtros-catalogo';
import { Pagina } from '../models/pagina';
import { Producto } from '../models/producto';
import { ProductoRequest } from '../models/producto-request';
import { Variante } from '../models/variante';
import { StockRequest, VarianteRequest } from '../models/variante-request';

@Injectable({
  providedIn: 'root'
})
export class ProductoService {
  private readonly API_URL = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /** Catálogo paginado con búsqueda, filtros y orden (GET /producto). */
  listar(filtros: FiltrosCatalogo, soloOfertas = false, soloNovedades = false): Observable<Pagina<Producto>> {
    const orden = [...ORDENES, ORDEN_NUEVOS].find((opcion) => opcion.valor === filtros.orden) ?? ORDENES[0];
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
    if (soloOfertas) {
      params = params.set('soloOfertas', true);
    }
    if (soloNovedades) {
      params = params.set('soloNovedades', true);
    }

    return this.http.get<Pagina<Producto>>(`${this.API_URL}/producto`, { params });
  }

  /** Las mejores ofertas por porcentaje de descuento (para la sección destacada). */
  destacadas(limite = 3): Observable<Producto[]> {
    const filtros: FiltrosCatalogo = {
      search: '',
      categoriaId: null,
      precioMin: null,
      precioMax: null,
      orden: 'descuento-desc',
      pagina: 0,
      tamanio: limite
    };
    return this.listar(filtros, true).pipe(map((pagina) => pagina.content));
  }

  /** Cuántos productos activos tiene la categoría (el back no lo informa en GET /categorias). */
  contarPorCategoria(idCategoria: number): Observable<number> {
    const params = new HttpParams().set('categoriaId', idCategoria).set('size', 1);
    return this.http
      .get<Pagina<Producto>>(`${this.API_URL}/producto`, { params })
      .pipe(map((pagina) => pagina.totalElements));
  }

  /** Un producto por id (GET /producto/{id}). Si no existe o está dado de baja, el back responde 404. */
  obtener(id: number): Observable<Producto> {
    return this.http.get<Producto>(`${this.API_URL}/producto/${id}`);
  }

  /** Solo ADMIN. El producto nace sin variantes ni imagen. */
  crear(producto: ProductoRequest): Observable<Producto> {
    return this.http.post<Producto>(`${this.API_URL}/producto`, producto);
  }

  /** Solo ADMIN. Reemplaza nombre, descripción, precio, descuento y categoría. */
  actualizar(id: number, producto: ProductoRequest): Observable<Producto> {
    return this.http.put<Producto>(`${this.API_URL}/producto/${id}`, producto);
  }

  /** Solo ADMIN. Lo pone o lo saca de "Novedades" del inicio, sin tocar el resto. */
  marcarNovedad(id: number, novedad: boolean): Observable<Producto> {
    return this.http.patch<Producto>(`${this.API_URL}/producto/${id}/novedad`, { novedad });
  }

  /** Solo ADMIN. Baja lógica: deja de verse en la tienda, pero los pedidos que lo incluyen no cambian. */
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/producto/${id}`);
  }

  /** Variantes activas del producto (GET /producto/{id}/variante). */
  listarVariantes(id: number): Observable<Variante[]> {
    return this.http.get<Variante[]>(`${this.API_URL}/producto/${id}/variante`);
  }

  /** Solo ADMIN. Alta de una variante con sus atributos y stock inicial. */
  crearVariante(idProducto: number, variante: VarianteRequest): Observable<Variante> {
    return this.http.post<Variante>(`${this.API_URL}/producto/${idProducto}/variante`, variante);
  }

  /** Solo ADMIN. Reemplaza los atributos y el stock de la variante. */
  actualizarVariante(idProducto: number, idVariante: number, variante: VarianteRequest): Observable<Variante> {
    return this.http.put<Variante>(`${this.API_URL}/producto/${idProducto}/variante/${idVariante}`, variante);
  }

  /** Solo ADMIN. Baja lógica: deja de ofrecerse, los pedidos que la incluyen no cambian. */
  eliminarVariante(idProducto: number, idVariante: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/producto/${idProducto}/variante/${idVariante}`);
  }

  /** Solo ADMIN. Suma o resta stock. Si la resta lo deja negativo, el back responde 400 "Stock insuficiente…". */
  ajustarStock(idProducto: number, idVariante: number, ajuste: StockRequest): Observable<Variante> {
    return this.http.patch<Variante>(`${this.API_URL}/producto/${idProducto}/variante/${idVariante}/stock`, ajuste);
  }
}
