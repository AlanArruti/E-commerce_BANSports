import { Component, computed, linkedSignal, signal, ChangeDetectionStrategy } from '@angular/core';
import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, debounceTime, map, of, startWith, switchMap } from 'rxjs';
import { PoteProducto, abreviarNombre } from '../../componente/pote-producto/pote-producto';
import { Categoria } from '../../models/categoria';
import { FiltrosCatalogo, ORDEN_NUEVOS, ORDENES, Orden } from '../../models/filtros-catalogo';
import { Pagina } from '../../models/pagina';
import { Producto } from '../../models/producto';
import { STOCK_BAJO } from '../../models/variante';
import { PrecioPipe } from '../../pipes/precio.pipe';
import { CategoriaService } from '../../services/categoria.service';
import { ProductoService } from '../../services/producto.service';
import { ErrorTraducido, traducirErrorBajaProducto, traducirErrorCatalogo } from '../../validaciones/errores-api';

const TAMANIO_PAGINA = 10;
/** Espera después de la última tecla antes de buscar. */
const DEMORA_FILTRO_MS = 350;
const PAGINAS_VISIBLES = 5;

interface EstadoListado {
  cargando: boolean;
  pagina: Pagina<Producto> | null;
  error: ErrorTraducido | null;
}

/** Deja solo los dígitos: el precio del filtro se escribe sin puntos ni signo $. */
function aNumero(texto: string): number | null {
  const digitos = texto.replace(/\D/g, '');
  return digitos ? Number(digitos) : null;
}

/** Listado de productos del panel (SCRUM-113): filtros, paginado, editar y dar de baja. */
@Component({
  selector: 'app-admin-productos',
  imports: [NgClass, ReactiveFormsModule, RouterLink, PoteProducto, PrecioPipe],
  templateUrl: './admin-productos.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './admin-productos.css',
  host: { '(document:keydown.escape)': 'cerrarBaja()' }
})
export class AdminProductos {
  protected readonly ordenes = [ORDEN_NUEVOS, ...ORDENES.filter((opcion) => opcion.valor !== 'relevantes')];
  protected readonly categorias;
  protected readonly estado;
  protected readonly paginaActual;
  protected readonly paginas;
  protected readonly resultado;
  protected readonly hayFiltros;
  protected readonly stockBajo = STOCK_BAJO;

  busqueda = new FormControl('', { nonNullable: true });
  categoria = new FormControl<number | null>(null);
  precioMin = new FormControl('', { nonNullable: true });
  precioMax = new FormControl('', { nonNullable: true });
  orden = new FormControl<Orden>('nuevos', { nonNullable: true });

  /** Confirmación de la última acción. Puede venir de la edición ("Guardaste los cambios…"). */
  protected readonly aviso = signal<string | null>(null);
  protected readonly productoBaja = signal<Producto | null>(null);
  protected readonly dandoDeBaja = signal(false);
  protected readonly errorBaja = signal<ErrorTraducido | null>(null);

  /** Emite para volver a pedir la página (después de una baja o con "Reintentar"). */
  private readonly recargar$ = new BehaviorSubject<void>(undefined);

  constructor(
    private productoService: ProductoService,
    categoriaService: CategoriaService
  ) {
    this.categorias = toSignal(categoriaService.listarActualizadas().pipe(catchError(() => of<Categoria[]>([]))), {
      initialValue: []
    });

    // El formulario de edición vuelve acá con el mensaje en el estado de la navegación.
    const avisoRecibido = history.state?.aviso;
    if (typeof avisoRecibido === 'string') {
      this.aviso.set(avisoRecibido);
      history.replaceState({ ...history.state, aviso: null }, '');
    }

    // Lo que se escribe espera un momento; los selects se aplican al toque.
    const escrito = (control: FormControl<string>) =>
      control.valueChanges.pipe(debounceTime(DEMORA_FILTRO_MS), startWith(control.value));
    const filtrosBase = toSignal(
      combineLatest([
        escrito(this.busqueda),
        this.categoria.valueChanges.pipe(startWith(this.categoria.value)),
        escrito(this.precioMin),
        escrito(this.precioMax),
        this.orden.valueChanges.pipe(startWith(this.orden.value))
      ]).pipe(
        map(([search, categoriaId, min, max, orden]) => ({
          search: search.trim(),
          categoriaId,
          precioMin: aNumero(min),
          precioMax: aNumero(max),
          orden
        }))
      ),
      { requireSync: true }
    );

    // Cambiar un filtro vuelve a la primera página.
    this.paginaActual = linkedSignal({ source: filtrosBase, computation: () => 0 });
    const consulta = computed<FiltrosCatalogo>(() => ({
      ...filtrosBase(),
      pagina: this.paginaActual(),
      tamanio: TAMANIO_PAGINA
    }));

    this.estado = toSignal(
      combineLatest([toObservable(consulta), this.recargar$]).pipe(
        switchMap(([filtros]) =>
          this.productoService.listar(filtros).pipe(
            map((pagina): EstadoListado => ({ cargando: false, pagina, error: null })),
            catchError((e: unknown) =>
              of<EstadoListado>({
                cargando: false,
                pagina: null,
                error:
                  e instanceof HttpErrorResponse
                    ? traducirErrorCatalogo(e)
                    : { titulo: 'No pudimos cargar los productos', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
              })
            ),
            startWith<EstadoListado>({ cargando: true, pagina: null, error: null })
          )
        )
      ),
      { initialValue: { cargando: true, pagina: null, error: null } }
    );

    this.hayFiltros = computed(() => {
      const filtros = filtrosBase();
      return !!filtros.search || filtros.categoriaId !== null || filtros.precioMin !== null || filtros.precioMax !== null;
    });

    this.resultado = computed(() => {
      const pagina = this.estado().pagina;
      if (!pagina || pagina.content.length === 0) {
        return null;
      }
      const desde = pagina.page * pagina.size + 1;
      return { desde, hasta: desde + pagina.content.length - 1, total: pagina.totalElements };
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

  irAPagina(pagina: number): void {
    this.paginaActual.set(pagina);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  limpiarFiltros(): void {
    this.busqueda.setValue('');
    this.categoria.setValue(null);
    this.precioMin.setValue('');
    this.precioMax.setValue('');
  }

  reintentar(): void {
    this.recargar$.next();
  }

  pedirBaja(producto: Producto): void {
    this.aviso.set(null);
    this.errorBaja.set(null);
    this.productoBaja.set(producto);
  }

  cerrarBaja(): void {
    if (!this.dandoDeBaja()) {
      this.productoBaja.set(null);
    }
  }

  confirmarBaja(): void {
    const producto = this.productoBaja();
    if (!producto) {
      return;
    }
    this.dandoDeBaja.set(true);
    this.errorBaja.set(null);
    this.productoService.eliminar(producto.id).subscribe({
      next: () => {
        this.dandoDeBaja.set(false);
        this.productoBaja.set(null);
        this.aviso.set(`Diste de baja "${producto.nombre}". Ya no se ve en la tienda.`);
        this.actualizarDespuesDeBaja();
      },
      error: (e: unknown) => {
        this.dandoDeBaja.set(false);
        this.errorBaja.set(
          e instanceof HttpErrorResponse
            ? traducirErrorBajaProducto(e)
            : { titulo: 'No pudimos dar de baja el producto', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
        if (e instanceof HttpErrorResponse && e.status === 404) {
          this.actualizarDespuesDeBaja();
        }
      }
    });
  }

  /** Suma del stock de las variantes activas. null si el producto todavía no tiene variantes. */
  protected stockTotal(producto: Producto): number | null {
    const activas = producto.variantes.filter((variante) => variante.activo);
    return activas.length ? activas.reduce((total, variante) => total + variante.stock, 0) : null;
  }

  protected abreviatura(producto: Producto): string {
    return abreviarNombre(producto.nombre);
  }

  /** Si se dio de baja el único producto de la última página, se vuelve a la anterior. */
  private actualizarDespuesDeBaja(): void {
    const pagina = this.estado().pagina;
    if (pagina && pagina.content.length === 1 && pagina.page > 0) {
      this.paginaActual.set(pagina.page - 1);
    } else {
      this.recargar$.next();
    }
  }
}
