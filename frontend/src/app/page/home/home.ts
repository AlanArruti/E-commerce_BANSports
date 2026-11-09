import { Component, ChangeDetectionStrategy, computed } from '@angular/core';
import { NgClass } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Observable, catchError, forkJoin, map, of, startWith, switchMap } from 'rxjs';
import { PoteProducto, abreviarNombre } from '../../componente/pote-producto/pote-producto';
import { TarjetaProducto } from '../../componente/tarjeta-producto/tarjeta-producto';
import { Categoria } from '../../models/categoria';
import { FiltrosCatalogo, Orden } from '../../models/filtros-catalogo';
import { Pagina } from '../../models/pagina';
import { Producto } from '../../models/producto';
import { rangoDePrecios } from '../../models/variante';
import { PrecioPipe } from '../../pipes/precio.pipe';
import { AuthService } from '../../services/auth.service';
import { CategoriaService } from '../../services/categoria.service';
import { ProductoService } from '../../services/producto.service';

interface Beneficio {
  titulo: string;
  detalle: string;
  icono: 'envio' | 'pago' | 'original' | 'asesoria';
}

/** Lo que carga una sección: mientras carga se muestra el esqueleto; con error o vacía, no se muestra. */
interface Carga<T> {
  cargando: boolean;
  datos: T;
}

interface CategoriaConCantidad {
  categoria: Categoria;
  cantidad: number;
}

/** Tarjetas por fila de productos. */
const POR_FILA = 4;

function filtros(orden: Orden, tamanio: number): FiltrosCatalogo {
  return { search: '', categoriaId: null, precioMin: null, precioMax: null, orden, pagina: 0, tamanio };
}

/** Pide algo para una sección: si falla, la sección queda vacía (y se oculta) en vez de romper el inicio. */
function cargar<T>(pedido: Observable<T>, vacio: T) {
  return pedido.pipe(
    map((datos): Carga<T> => ({ cargando: false, datos })),
    catchError(() => of<Carga<T>>({ cargando: false, datos: vacio })),
    startWith<Carga<T>>({ cargando: true, datos: vacio })
  );
}

/**
 * Inicio (SCRUM-149): hero con la mejor oferta, categorías reales, ofertas destacadas y
 * lo más nuevo. Cada sección carga por separado y se oculta si no hay nada para mostrar.
 */
@Component({
  selector: 'app-home',
  imports: [NgClass, RouterLink, PoteProducto, PrecioPipe, TarjetaProducto],
  templateUrl: './home.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './home.css'
})
export class Home {
  protected readonly esqueletos = Array.from({ length: POR_FILA }, (_, i) => i);

  protected readonly beneficios: Beneficio[] = [
    { titulo: 'Envío a todo el país', detalle: 'Seguimiento con Correo Argentino', icono: 'envio' },
    { titulo: 'Pagá con Mercado Pago', detalle: 'Tarjeta, débito o dinero en cuenta', icono: 'pago' },
    { titulo: 'Productos originales', detalle: 'Con lote y vencimiento visibles', icono: 'original' },
    { titulo: 'Te asesoramos', detalle: 'Elegí el suplemento para tu objetivo', icono: 'asesoria' }
  ];

  /** Ofertas por mayor descuento: la primera va al hero y las siguientes a la fila. */
  private readonly ofertas;
  protected readonly nuevos;
  protected readonly categorias;

  protected readonly cargandoOfertas = computed(() => this.ofertas().cargando);
  protected readonly ofertaHero = computed(() => this.ofertas().datos.content[0] ?? null);
  protected readonly ofertasFila = computed(() => this.ofertas().datos.content.slice(1, POR_FILA + 1));
  protected readonly totalOfertas = computed(() => this.ofertas().datos.totalElements);

  /** Precios del hero: el más barato de las variantes, con el de lista para tacharlo. */
  protected readonly preciosHero = computed(() => {
    const producto = this.ofertaHero();
    if (!producto) return null;
    const rango = rangoDePrecios(producto);
    return { vigente: rango.minimo, lista: rango.listaMinimo, desde: rango.minimo < rango.maximo };
  });

  constructor(
    protected authService: AuthService,
    productoService: ProductoService,
    categoriaService: CategoriaService
  ) {
    const sinProductos: Pagina<Producto> = { content: [], totalElements: 0, totalPages: 0, page: 0, size: 0 };

    this.ofertas = toSignal(
      cargar(productoService.listar(filtros('descuento-desc', POR_FILA + 1), true), sinProductos),
      { initialValue: { cargando: true, datos: sinProductos } }
    );

    this.nuevos = toSignal(
      cargar(productoService.listar(filtros('nuevos', POR_FILA)).pipe(map((p) => p.content)), [] as Producto[]),
      { initialValue: { cargando: true, datos: [] as Producto[] } }
    );

    // Cada categoría con cuántos productos tiene; las vacías no se muestran (llevarían a un catálogo sin nada).
    this.categorias = toSignal(
      cargar(
        categoriaService.listar().pipe(
          switchMap((lista) =>
            lista.length
              ? forkJoin(
                  lista.map((categoria) =>
                    productoService.contarPorCategoria(categoria.id).pipe(
                      map((cantidad): CategoriaConCantidad => ({ categoria, cantidad })),
                      catchError(() => of<CategoriaConCantidad>({ categoria, cantidad: 0 }))
                    )
                  )
                )
              : of([])
          ),
          map((lista) => lista.filter((c) => c.cantidad > 0))
        ),
        [] as CategoriaConCantidad[]
      ),
      { initialValue: { cargando: true, datos: [] as CategoriaConCantidad[] } }
    );
  }

  /** La categoría con más productos va resaltada en verde. */
  protected readonly idCategoriaDestacada = computed(() => {
    const lista = this.categorias().datos;
    return lista.length ? lista.reduce((a, b) => (b.cantidad > a.cantidad ? b : a)).categoria.id : null;
  });

  protected numero(indice: number): string {
    return String(indice + 1).padStart(2, '0');
  }

  protected abreviatura(producto: Producto): string {
    return abreviarNombre(producto.nombre);
  }
}
