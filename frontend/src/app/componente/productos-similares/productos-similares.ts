import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Observable, catchError, map, of, startWith, switchMap } from 'rxjs';
import { TarjetaProducto } from '../tarjeta-producto/tarjeta-producto';
import { Categoria } from '../../models/categoria';
import { FiltrosCatalogo } from '../../models/filtros-catalogo';
import { Producto } from '../../models/producto';
import { ProductoService } from '../../services/producto.service';

/** Cuántas tarjetas muestra la fila. */
const CANTIDAD = 4;
/**
 * Cuántos productos de la categoría se piden para elegir los 4. Vienen ordenados por
 * descuento, así que las ofertas entran aunque la categoría sea grande.
 */
const CANDIDATOS = 24;

interface Similares {
  cargando: boolean;
  /** 'categoria': de la misma categoría. 'ofertas': la categoría no tenía otros. */
  origen: 'categoria' | 'ofertas';
  categoria: Categoria | null;
  productos: Producto[];
}

const CARGANDO: Similares = { cargando: true, origen: 'categoria', categoria: null, productos: [] };
const VACIO: Similares = { cargando: false, origen: 'categoria', categoria: null, productos: [] };

function stockTotal(producto: Producto): number {
  return producto.variantes.filter((v) => v.activo).reduce((total, v) => total + v.stock, 0);
}

/** Primero los que tienen stock; entre ellos, los de mayor descuento. Si empatan, queda el orden del back. */
function priorizar(productos: Producto[]): Producto[] {
  return productos
    .map((producto, indice) => ({ producto, indice, conStock: stockTotal(producto) > 0, descuento: producto.descuento ?? 0 }))
    .sort(
      (a, b) =>
        Number(b.conStock) - Number(a.conStock) ||
        b.descuento - a.descuento ||
        a.indice - b.indice
    )
    .map((item) => item.producto);
}

/**
 * Fila de hasta 4 productos parecidos al del detalle: de la misma categoría, sin el actual,
 * con stock y en oferta primero. Si la categoría no tiene otros, muestra ofertas destacadas.
 * Si algo falla, la sección no aparece: es contenido de apoyo y no tiene que molestar.
 */
@Component({
  selector: 'app-productos-similares',
  imports: [RouterLink, TarjetaProducto],
  templateUrl: './productos-similares.html',
  styleUrl: './productos-similares.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductosSimilares {
  readonly producto = input.required<Producto>();

  protected readonly esqueletos = Array.from({ length: CANTIDAD }, (_, i) => i);
  protected readonly similares;

  constructor(private productoService: ProductoService) {
    this.similares = toSignal(
      toObservable(this.producto).pipe(
        switchMap((actual) =>
          this.deLaCategoria(actual).pipe(
            switchMap((productos) =>
              productos.length
                ? of<Similares>({ cargando: false, origen: 'categoria', categoria: actual.categoria, productos })
                : this.ofertas(actual).pipe(
                    map((ofertas): Similares => ({ cargando: false, origen: 'ofertas', categoria: null, productos: ofertas }))
                  )
            ),
            catchError(() => of(VACIO)),
            startWith(CARGANDO)
          )
        )
      ),
      { initialValue: CARGANDO }
    );
  }

  /** El link de la tarjeta lleva a otro producto, pero el detalle no cambia de página: se vuelve arriba. */
  protected alElegir(evento: MouseEvent): void {
    if ((evento.target as HTMLElement).closest('a')) {
      window.scrollTo({ top: 0 });
    }
  }

  private deLaCategoria(actual: Producto): Observable<Producto[]> {
    if (!actual.categoria) {
      return of([]);
    }
    const filtros: FiltrosCatalogo = {
      search: '',
      categoriaId: actual.categoria.id,
      precioMin: null,
      precioMax: null,
      orden: 'descuento-desc',
      pagina: 0,
      tamanio: CANDIDATOS
    };
    return this.productoService.listar(filtros).pipe(
      map((pagina) => priorizar(pagina.content.filter((p) => p.id !== actual.id)).slice(0, CANTIDAD))
    );
  }

  private ofertas(actual: Producto): Observable<Producto[]> {
    return this.productoService
      .destacadas(CANTIDAD + 1)
      .pipe(map((productos) => productos.filter((p) => p.id !== actual.id).slice(0, CANTIDAD)));
  }
}
