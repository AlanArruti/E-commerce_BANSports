import { ChangeDetectionStrategy, Component, ElementRef, computed, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { NavigationEnd, Router } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, filter, map, of, switchMap, tap } from 'rxjs';
import { PoteProducto, abreviarNombre } from '../pote-producto/pote-producto';
import { FiltrosCatalogo } from '../../models/filtros-catalogo';
import { Producto } from '../../models/producto';
import { rangoDePrecios } from '../../models/variante';
import { PrecioPipe } from '../../pipes/precio.pipe';
import { ProductoService } from '../../services/producto.service';

/** Desde cuántos caracteres se buscan sugerencias. */
const MINIMO_BUSQUEDA = 2;
const DEMORA_MS = 300;
const CANTIDAD_SUGERENCIAS = 5;

interface Resultado {
  texto: string;
  productos: Producto[];
  total: number;
  /** El back no respondió: se cierra la lista y Enter sigue llevando al catálogo. */
  error: boolean;
}

/** Nombre partido en tramos para resaltar lo que coincide con lo escrito. */
interface Tramo {
  texto: string;
  coincide: boolean;
}

/** Saca tildes y pasa a minúsculas, sin cambiar el largo del texto (para ubicar la coincidencia). */
function normalizar(texto: string): string {
  return [...texto].map((letra) => letra.normalize('NFD').charAt(0).toLowerCase()).join('');
}

/**
 * Buscador del header con sugerencias (patrón combobox de ARIA, como el de localidades).
 * Mientras se escribe muestra hasta 5 productos; Enter abre el marcado o, si no hay
 * ninguno marcado, el catálogo filtrado.
 */
@Component({
  selector: 'app-buscador-productos',
  imports: [ReactiveFormsModule, PoteProducto, PrecioPipe],
  templateUrl: './buscador-productos.html',
  styleUrl: './buscador-productos.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BuscadorProductos {
  readonly busqueda = new FormControl('', { nonNullable: true });

  protected readonly resultado = signal<Resultado | null>(null);
  protected readonly abierta = signal(false);
  /** Opción marcada con el teclado o el mouse. -1: ninguna (Enter va al catálogo). */
  protected readonly activa = signal(-1);
  protected readonly buscando = signal(false);

  protected readonly productos = computed(() => this.resultado()?.productos ?? []);
  /** Las opciones son los productos y, al final, "Ver todos los resultados". */
  protected readonly indiceVerTodos = computed(() => this.productos().length);
  protected readonly cantidadOpciones = computed(() => (this.productos().length ? this.productos().length + 1 : 0));
  protected readonly anuncio = computed(() => {
    const r = this.resultado();
    if (!this.abierta() || !r) return '';
    if (!r.productos.length) return `No hay productos que coincidan con "${r.texto}".`;
    return `${r.productos.length} ${r.productos.length === 1 ? 'producto sugerido' : 'productos sugeridos'}. Usá las flechas para elegir.`;
  });

  private readonly campo = viewChild.required<ElementRef<HTMLInputElement>>('campo');

  constructor(
    private productoService: ProductoService,
    private router: Router
  ) {
    this.busqueda.valueChanges
      .pipe(
        map((texto) => texto.trim()),
        tap((texto) => {
          // Lo que se ve tiene que corresponder a lo escrito: si se borra, se cierra al toque.
          if (texto.length < MINIMO_BUSQUEDA) this.cerrar();
        }),
        debounceTime(DEMORA_MS),
        distinctUntilChanged(),
        switchMap((texto) => {
          if (texto.length < MINIMO_BUSQUEDA) {
            this.buscando.set(false);
            return of(null);
          }
          this.buscando.set(true);
          return this.productoService.listar(this.filtros(texto)).pipe(
            map((pagina): Resultado => ({ texto, productos: pagina.content, total: pagina.totalElements, error: false })),
            catchError(() => of<Resultado>({ texto, productos: [], total: 0, error: true }))
          );
        }),
        takeUntilDestroyed()
      )
      .subscribe((resultado) => {
        this.buscando.set(false);
        this.resultado.set(resultado && !resultado.error ? resultado : null);
        this.activa.set(-1);
        // Solo se abre si el usuario sigue en el buscador (la respuesta puede llegar después de salir).
        this.abierta.set(!!this.resultado() && document.activeElement === this.campo().nativeElement);
      });

    // El buscador muestra lo que se está buscando en el catálogo, y se vacía al salir de él.
    this.router.events
      .pipe(
        filter((evento) => evento instanceof NavigationEnd),
        takeUntilDestroyed()
      )
      .subscribe((evento) => {
        const url = this.router.parseUrl(evento.urlAfterRedirects);
        const enCatalogo = evento.urlAfterRedirects.split('?')[0] === '/productos';
        // Sin emitir: cambiar de página no tiene que abrir sugerencias.
        this.busqueda.setValue(enCatalogo ? (url.queryParamMap.get('search') ?? '') : '', { emitEvent: false });
        this.resultado.set(null);
        this.cerrar();
      });
  }

  // ── Teclado ───────────────────────────────────────────

  alTeclear(evento: KeyboardEvent): void {
    const total = this.cantidadOpciones();
    if (evento.key === 'ArrowDown' && total) {
      evento.preventDefault();
      this.abierta.set(true);
      this.activa.update((i) => (i + 1 >= total ? -1 : i + 1));
    } else if (evento.key === 'ArrowUp' && total) {
      evento.preventDefault();
      this.abierta.set(true);
      this.activa.update((i) => (i < 0 ? total - 1 : i - 1));
    } else if (evento.key === 'Escape' && this.abierta()) {
      evento.preventDefault();
      this.cerrar();
    }
  }

  /** Enter (submit del form): el producto marcado o, si no hay ninguno, el catálogo filtrado. */
  alEnviar(evento: Event): void {
    evento.preventDefault();
    const i = this.activa();
    const producto = this.abierta() && i >= 0 ? this.productos()[i] : undefined;
    if (producto) {
      this.irAProducto(producto);
    } else {
      this.verTodos();
    }
  }

  // ── Mouse ─────────────────────────────────────────────

  /** mousedown (no click) para que corra antes del blur del input. */
  alApretarProducto(evento: MouseEvent, producto: Producto): void {
    if (evento.button !== 0) return;
    evento.preventDefault();
    this.irAProducto(producto);
  }

  alApretarVerTodos(evento: MouseEvent): void {
    if (evento.button !== 0) return;
    evento.preventDefault();
    this.verTodos();
  }

  alEnfocar(): void {
    if (this.resultado() && this.busqueda.value.trim().length >= MINIMO_BUSQUEDA) {
      this.abierta.set(true);
    }
  }

  alSalir(): void {
    this.cerrar();
  }

  // ── Vista ─────────────────────────────────────────────

  protected tramos(nombre: string): Tramo[] {
    const buscado = normalizar(this.resultado()?.texto ?? '');
    const desde = buscado ? normalizar(nombre).indexOf(buscado) : -1;
    if (desde < 0) return [{ texto: nombre, coincide: false }];
    const hasta = desde + buscado.length;
    return [
      { texto: nombre.slice(0, desde), coincide: false },
      { texto: nombre.slice(desde, hasta), coincide: true },
      { texto: nombre.slice(hasta), coincide: false }
    ].filter((t) => t.texto);
  }

  protected precios(producto: Producto) {
    const rango = rangoDePrecios(producto);
    return {
      vigente: rango.minimo,
      lista: (producto.descuento ?? 0) > 0 ? rango.listaMinimo : null,
      desde: rango.minimo < rango.maximo
    };
  }

  protected abreviatura(producto: Producto): string {
    return abreviarNombre(producto.nombre);
  }

  // ── Acciones ──────────────────────────────────────────

  private irAProducto(producto: Producto): void {
    this.cerrar();
    this.campo().nativeElement.blur();
    this.router.navigate(['/productos', producto.id]);
  }

  /** Catálogo completo filtrado por lo escrito (sin los filtros que hubiera antes). */
  private verTodos(): void {
    const texto = this.busqueda.value.trim();
    this.cerrar();
    this.router.navigate(['/productos'], { queryParams: texto ? { search: texto } : {} });
  }

  private cerrar(): void {
    this.abierta.set(false);
    this.activa.set(-1);
  }

  private filtros(texto: string): FiltrosCatalogo {
    return {
      search: texto,
      categoriaId: null,
      precioMin: null,
      precioMax: null,
      orden: 'relevantes',
      pagina: 0,
      tamanio: CANTIDAD_SUGERENCIAS
    };
  }
}
