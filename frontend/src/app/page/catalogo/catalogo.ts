import { Component, computed, effect, signal, ChangeDetectionStrategy } from '@angular/core';
import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, ParamMap, Params, Router, RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, debounceTime, map, merge, of, startWith, switchMap } from 'rxjs';
import { TarjetaProducto } from '../../componente/tarjeta-producto/tarjeta-producto';
import { FiltrosCatalogo, ORDENES, Orden } from '../../models/filtros-catalogo';
import { Pagina } from '../../models/pagina';
import { Producto } from '../../models/producto';
import { formatearPrecio } from '../../pipes/precio.pipe';
import { CategoriaService } from '../../services/categoria.service';
import { ProductoService } from '../../services/producto.service';
import { ErrorTraducido, traducirErrorCatalogo } from '../../validaciones/errores-api';
import { MSJ_PRECIO_RANGO } from '../../validaciones/mensajes';

const TAMANIO_PAGINA = 12;
/** Espera después de la última tecla antes de aplicar el filtro de precio. */
const DEMORA_PRECIO_MS = 600;
/** Cantidad máxima de números que se muestran en la paginación. */
const PAGINAS_VISIBLES = 5;

interface EstadoCatalogo {
  cargando: boolean;
  pagina: Pagina<Producto> | null;
  error: ErrorTraducido | null;
}

interface Chip {
  texto: string;
  /** Parámetros de la URL que se borran al quitar el chip. */
  quitar: Params;
}

/** Lee los filtros de la URL. Cualquier valor inválido se ignora en vez de romper la página. */
function leerFiltros(params: ParamMap): FiltrosCatalogo {
  const numero = (clave: string): number | null => {
    const texto = params.get(clave);
    const valor = Number(texto);
    return texto && Number.isFinite(valor) && valor >= 0 ? valor : null;
  };
  const orden = params.get('orden') as Orden | null;
  const pagina = Math.floor(numero('pagina') ?? 1);

  return {
    search: (params.get('search') ?? '').trim(),
    categoriaId: numero('categoria'),
    precioMin: numero('precioMin'),
    precioMax: numero('precioMax'),
    orden: ORDENES.some((opcion) => opcion.valor === orden) ? orden! : 'relevantes',
    pagina: Math.max(pagina, 1) - 1,
    tamanio: TAMANIO_PAGINA
  };
}

/** Deja solo los dígitos: el precio se escribe sin puntos ni signo $. */
function aNumero(texto: string): number | null {
  const digitos = texto.replace(/\D/g, '');
  return digitos ? Number(digitos) : null;
}

@Component({
  selector: 'app-catalogo',
  imports: [NgClass, ReactiveFormsModule, RouterLink, TarjetaProducto],
  templateUrl: './catalogo.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './catalogo.css',
  host: { '(document:keydown.escape)': 'cerrarFiltros()' }
})
export class Catalogo {
  protected readonly ordenes = ORDENES;
  protected readonly esqueletos = [1, 2, 3, 4, 5, 6];
  protected readonly esOfertas: boolean;

  /** Mejores ofertas por descuento. Solo se cargan en /ofertas. */
  protected readonly destacadas = signal<Producto[]>([]);
  protected readonly cargandoDestacadas = signal(false);

  precioMin = new FormControl('', { nonNullable: true });
  precioMax = new FormControl('', { nonNullable: true });

  /** Panel de filtros abierto (solo en mobile, donde se muestra como drawer). */
  protected readonly filtrosAbiertos = signal(false);

  protected readonly filtros;
  protected readonly categorias;
  protected readonly estado;
  protected readonly errorPrecio;
  protected readonly titulo;
  protected readonly chips;
  protected readonly resultado;
  protected readonly paginas;

  /** Emite para volver a pedir los productos (botón "Reintentar"). */
  private readonly recargar$ = new BehaviorSubject<void>(undefined);

  constructor(
    private productoService: ProductoService,
    private categoriaService: CategoriaService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.esOfertas = this.route.snapshot.data['ofertas'] === true;
    const filtros$ = this.route.queryParamMap.pipe(map(leerFiltros));
    this.filtros = toSignal(filtros$, { requireSync: true });
    this.categorias = toSignal(this.categoriaService.listar(), { initialValue: [] });

    if (this.esOfertas) {
      this.cargarDestacadas();
    }

    // Cada cambio de filtros en la URL vuelve a pedir el catálogo (y cancela el pedido anterior).
    this.estado = toSignal(
      combineLatest([filtros$, this.recargar$]).pipe(
        switchMap(([filtros]) =>
          this.productoService.listar(filtros, this.esOfertas).pipe(
            map((pagina): EstadoCatalogo => ({ cargando: false, pagina, error: null })),
            catchError((e: unknown) =>
              of<EstadoCatalogo>({
                cargando: false,
                pagina: null,
                error:
                  e instanceof HttpErrorResponse
                    ? traducirErrorCatalogo(e)
                    : { titulo: 'No pudimos cargar los productos', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
              })
            ),
            startWith<EstadoCatalogo>({ cargando: true, pagina: null, error: null })
          )
        )
      ),
      { initialValue: { cargando: true, pagina: null, error: null } }
    );

    // Los inputs de precio muestran lo que dice la URL (por ejemplo, al quitar un chip o ir atrás).
    // Solo se pisa el input cuyo valor cambió en la URL: si el usuario estaba escribiendo en el otro,
    // no se le borra. Se emite el cambio para que el precio pendiente se aplique (ver aplicarPrecio).
    let anterior: Pick<FiltrosCatalogo, 'precioMin' | 'precioMax'> | null = null;
    effect(() => {
      const { precioMin, precioMax } = this.filtros();
      if (anterior?.precioMin !== precioMin) {
        this.precioMin.setValue(precioMin === null ? '' : String(precioMin));
      }
      if (anterior?.precioMax !== precioMax) {
        this.precioMax.setValue(precioMax === null ? '' : String(precioMax));
      }
      anterior = { precioMin, precioMax };
    });

    const valorMin = toSignal(this.precioMin.valueChanges, { initialValue: '' });
    const valorMax = toSignal(this.precioMax.valueChanges, { initialValue: '' });
    this.errorPrecio = computed(() => {
      const min = aNumero(valorMin());
      const max = aNumero(valorMax());
      return min !== null && max !== null && min > max ? MSJ_PRECIO_RANGO : null;
    });

    // El precio se aplica solo, cuando el usuario deja de escribir.
    merge(this.precioMin.valueChanges, this.precioMax.valueChanges)
      .pipe(debounceTime(DEMORA_PRECIO_MS), takeUntilDestroyed())
      .subscribe(() => this.aplicarPrecio());

    this.titulo = computed(() => {
      const categoria = this.nombreCategoria(this.filtros().categoriaId);
      if (this.esOfertas) {
        return categoria ? `Ofertas de ${categoria}` : 'Ofertas';
      }
      return categoria ?? 'Catálogo';
    });

    this.chips = computed<Chip[]>(() => {
      const filtros = this.filtros();
      const chips: Chip[] = [];
      if (filtros.search) {
        chips.push({ texto: `“${filtros.search}”`, quitar: { search: null } });
      }
      const categoria = this.nombreCategoria(filtros.categoriaId);
      if (categoria) {
        chips.push({ texto: categoria, quitar: { categoria: null } });
      }
      if (filtros.precioMin !== null) {
        chips.push({ texto: `Desde ${formatearPrecio(filtros.precioMin)}`, quitar: { precioMin: null } });
      }
      if (filtros.precioMax !== null) {
        chips.push({ texto: `Hasta ${formatearPrecio(filtros.precioMax)}`, quitar: { precioMax: null } });
      }
      return chips;
    });

    this.resultado = computed(() => {
      const pagina = this.estado().pagina;
      if (!pagina) {
        return null;
      }
      const total = pagina.totalElements;
      if (total === 0 || pagina.content.length === 0) {
        return { desde: 0, hasta: 0, total };
      }
      const desde = pagina.page * pagina.size + 1;
      return { desde, hasta: desde + pagina.content.length - 1, total };
    });

    // Ventana de números alrededor de la página actual: 1 2 [3] 4 5.
    this.paginas = computed(() => {
      const pagina = this.estado().pagina;
      if (!pagina || pagina.totalPages <= 1) {
        return [];
      }
      const inicio = Math.max(0, Math.min(pagina.page - 2, pagina.totalPages - PAGINAS_VISIBLES));
      const fin = Math.min(pagina.totalPages, inicio + PAGINAS_VISIBLES);
      return Array.from({ length: fin - inicio }, (_, i) => inicio + i);
    });
  }

  seleccionarCategoria(id: number | null): void {
    this.actualizar({ categoria: id });
  }

  ordenar(evento: Event): void {
    const valor = (evento.target as HTMLSelectElement).value as Orden;
    this.actualizar({ orden: valor === 'relevantes' ? null : valor });
  }

  quitarChip(chip: Chip): void {
    this.actualizar(chip.quitar);
  }

  limpiarFiltros(): void {
    this.actualizar({ search: null, categoria: null, precioMin: null, precioMax: null });
  }

  /** @param pagina página empezando en 0, como la devuelve el back. */
  irAPagina(pagina: number): void {
    this.actualizar({ pagina: pagina === 0 ? null : pagina + 1 }, false);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  reintentar(): void {
    this.recargar$.next();
  }

  private cargarDestacadas(): void {
    this.cargandoDestacadas.set(true);
    this.productoService.destacadas().subscribe({
      next: (productos) => {
        this.destacadas.set(productos);
        this.cargandoDestacadas.set(false);
      },
      error: () => {
        this.destacadas.set([]);
        this.cargandoDestacadas.set(false);
      }
    });
  }

  abrirFiltros(): void {
    this.filtrosAbiertos.set(true);
  }

  cerrarFiltros(): void {
    this.filtrosAbiertos.set(false);
  }

  private aplicarPrecio(): void {
    if (this.errorPrecio()) {
      return;
    }
    const min = aNumero(this.precioMin.value);
    const max = aNumero(this.precioMax.value);
    const filtros = this.filtros();
    if (min !== filtros.precioMin || max !== filtros.precioMax) {
      this.actualizar({ precioMin: min, precioMax: max });
    }
  }

  private nombreCategoria(id: number | null): string | null {
    return id === null ? null : (this.categorias().find((categoria) => categoria.id === id)?.nombre ?? null);
  }

  /**
   * Cambia los filtros en la URL (así se pueden compartir y funciona el botón "atrás").
   * Un valor null borra el parámetro. Cambiar un filtro vuelve a la primera página.
   */
  private actualizar(cambios: Params, volverAPrimeraPagina = true): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: volverAPrimeraPagina ? { ...cambios, pagina: null } : cambios,
      queryParamsHandling: 'merge'
    });
  }
}
