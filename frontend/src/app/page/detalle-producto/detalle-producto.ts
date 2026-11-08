import { Component, computed, effect, linkedSignal, signal, ChangeDetectionStrategy } from '@angular/core';
import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, forkJoin, map, of, startWith, switchMap } from 'rxjs';
import { NoEncontrado } from '../../componente/no-encontrado/no-encontrado';
import { PoteProducto, abreviarNombre } from '../../componente/pote-producto/pote-producto';
import { Producto } from '../../models/producto';
import {
  STOCK_BAJO,
  Variante,
  atributosOrdenados,
  etiquetaAtributo,
  precioFinalDe,
  precioListaDe,
  rangoDePrecios
} from '../../models/variante';
import { PrecioPipe } from '../../pipes/precio.pipe';
import { AuthService } from '../../services/auth.service';
import { CarritoService } from '../../services/carrito.service';
import { FavoritoService } from '../../services/favorito.service';
import { ProductoService } from '../../services/producto.service';
import { ErrorTraducido, traducirErrorCarrito, traducirErrorProducto } from '../../validaciones/errores-api';
import { ErrorCarga } from '../../componente/error-carga/error-carga';

interface EstadoDetalle {
  cargando: boolean;
  producto: Producto | null;
  variantes: Variante[];
  noEncontrado: boolean;
  error: ErrorTraducido | null;
}

/** Un valor elegible de un atributo (por ejemplo, sabor "Chocolate"). */
interface OpcionVariante {
  valor: string;
  elegida: boolean;
  /** Ninguna variante con este valor tiene stock (por ejemplo, el sabor Frutilla en cualquier presentación). */
  agotada: boolean;
}

interface GrupoVariante {
  clave: string;
  etiqueta: string;
  opciones: OpcionVariante[];
}

type Seleccion = Record<string, string>;

const CARGANDO: EstadoDetalle = { cargando: true, producto: null, variantes: [], noEncontrado: false, error: null };
const NO_ENCONTRADO: EstadoDetalle = { cargando: false, producto: null, variantes: [], noEncontrado: true, error: null };

/** La variante tiene exactamente los atributos elegidos. */
function coincide(variante: Variante, seleccion: Seleccion): boolean {
  const claves = Object.keys(seleccion);
  return (
    claves.length === Object.keys(variante.atributos).length &&
    claves.every((clave) => variante.atributos[clave] === seleccion[clave])
  );
}

/**
 * Al entrar se elige la variante de la URL (?variante=, la que había elegido antes de ir al login),
 * si no la primera con stock, y si ninguna tiene, la primera.
 */
function varianteInicial(variantes: Variante[], idDeLaUrl: string | null): Variante | null {
  return (
    variantes.find((variante) => String(variante.id) === idDeLaUrl) ??
    variantes.find((variante) => variante.stock > 0) ??
    variantes[0] ??
    null
  );
}

@Component({
  selector: 'app-detalle-producto',
  imports: [ErrorCarga, NgClass, RouterLink, NoEncontrado, PoteProducto, PrecioPipe],
  templateUrl: './detalle-producto.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './detalle-producto.css'
})
export class DetalleProducto {
  protected readonly estado;
  protected readonly abreviatura;
  protected readonly seleccion;
  protected readonly varianteElegida;
  protected readonly grupos;
  protected readonly productoAgotado;
  protected readonly stockBajo;
  protected readonly tieneOferta = computed(() => (this.estado().producto?.descuento ?? 0) > 0);
  protected readonly descuento = computed(() => Math.round(this.estado().producto?.descuento ?? 0));
  /** Precio de la variante elegida (SCRUM-154). Sin variante elegida, el más barato. */
  protected readonly precioVigente = computed(() => {
    const producto = this.estado().producto;
    if (!producto) return 0;
    const variante = this.varianteElegida();
    return variante ? precioFinalDe(producto, variante) : rangoDePrecios(producto).minimo;
  });
  /** Precio sin la oferta, para mostrarlo tachado. */
  protected readonly precioLista = computed(() => {
    const producto = this.estado().producto;
    if (!producto) return 0;
    const variante = this.varianteElegida();
    return variante ? precioListaDe(producto, variante) : rangoDePrecios(producto).listaMinimo;
  });
  /** Todavía no eligió variante y no todas cuestan lo mismo: se muestra "Desde". */
  protected readonly esPrecioDesde = computed(() => {
    const producto = this.estado().producto;
    if (!producto || this.varianteElegida()) return false;
    const rango = rangoDePrecios(producto);
    return rango.minimo < rango.maximo;
  });
  /** La imagen del back no cargó: se muestra el dibujo del pote. */
  protected readonly imagenFallo;

  /** Unidades a agregar al carrito. Vuelve a 1 al cambiar de variante. */
  protected readonly cantidad;
  /** Unidades de la variante elegida que ya están en el carrito. */
  protected readonly enCarrito;
  /** Cuántas más se pueden agregar sin pasar el stock. */
  protected readonly maximo;
  protected readonly agregando = signal(false);
  /** Resultado del último "Agregar al carrito". Se borra al cambiar de variante. */
  protected readonly resultadoAgregar;

  /** Emite para volver a pedir el producto (botón "Reintentar"). */
  private readonly recargar$ = new BehaviorSubject<void>(undefined);

  constructor(
    private productoService: ProductoService,
    private carritoService: CarritoService,
    protected favoritoService: FavoritoService,
    protected authService: AuthService,
    private route: ActivatedRoute,
    private title: Title
  ) {
    const id$ = this.route.paramMap.pipe(map((params) => params.get('id')));

    // Producto y variantes juntos. Si el id no es un número, ni se consulta: no existe.
    this.estado = toSignal(
      combineLatest([id$, this.recargar$]).pipe(
        switchMap(([texto]) => {
          const id = Number(texto);
          // Siempre un objeto nuevo: si quedara el mismo, el título no se actualiza entre dos productos inexistentes.
          if (!Number.isInteger(id) || id <= 0) {
            return of({ ...NO_ENCONTRADO });
          }
          return forkJoin([this.productoService.obtener(id), this.productoService.listarVariantes(id)]).pipe(
            map(([producto, variantes]): EstadoDetalle => ({ ...CARGANDO, cargando: false, producto, variantes })),
            catchError((e: unknown) => {
              if (e instanceof HttpErrorResponse && e.status === 404) {
                return of({ ...NO_ENCONTRADO });
              }
              return of<EstadoDetalle>({
                ...CARGANDO,
                cargando: false,
                error:
                  e instanceof HttpErrorResponse
                    ? traducirErrorProducto(e)
                    : { titulo: 'No pudimos cargar el producto', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
              });
            }),
            startWith(CARGANDO)
          );
        })
      ),
      { initialValue: CARGANDO }
    );

    this.abreviatura = computed(() => abreviarNombre(this.estado().producto?.nombre ?? ''));
    this.imagenFallo = linkedSignal({ source: () => this.estado().producto?.id, computation: () => false });

    // Atributos elegidos. Vuelve a la variante inicial cada vez que se carga otro producto.
    this.seleccion = linkedSignal<Seleccion>(() => ({
      ...varianteInicial(this.estado().variantes, this.route.snapshot.queryParamMap.get('variante'))?.atributos
    }));

    this.varianteElegida = computed(
      () => this.estado().variantes.find((variante) => coincide(variante, this.seleccion())) ?? null
    );

    this.cantidad = linkedSignal({ source: this.varianteElegida, computation: () => 1 });
    this.resultadoAgregar = linkedSignal<Variante | null, { ok: boolean; error: ErrorTraducido | null } | null>({
      source: this.varianteElegida,
      computation: () => null
    });
    this.enCarrito = computed(() => {
      const variante = this.varianteElegida();
      return variante ? this.carritoService.cantidadDeVariante(variante.id) : 0;
    });
    this.maximo = computed(() => Math.max((this.varianteElegida()?.stock ?? 0) - this.enCarrito(), 0));

    // Un grupo por atributo, con sus valores en el orden en que aparecen: Presentación [1 kg], Sabor [Chocolate, ...].
    this.grupos = computed<GrupoVariante[]>(() => {
      const variantes = this.estado().variantes;
      const seleccion = this.seleccion();
      const valoresPorClave = new Map<string, string[]>();
      for (const variante of variantes) {
        for (const [clave, valor] of atributosOrdenados(variante)) {
          const valores = valoresPorClave.get(clave) ?? [];
          if (!valores.includes(valor)) {
            valores.push(valor);
          }
          valoresPorClave.set(clave, valores);
        }
      }
      return [...valoresPorClave].map(([clave, valores]) => ({
        clave,
        etiqueta: etiquetaAtributo(clave),
        opciones: valores.map((valor) => ({
          valor,
          elegida: seleccion[clave] === valor,
          agotada: variantes.every((variante) => variante.atributos[clave] !== valor || variante.stock === 0)
        }))
      }));
    });

    this.productoAgotado = computed(() => this.estado().variantes.every((variante) => variante.stock === 0));

    this.stockBajo = computed(() => {
      const stock = this.varianteElegida()?.stock ?? 0;
      return stock > 0 && stock <= STOCK_BAJO;
    });

    // El título de la pestaña muestra el producto.
    effect(() => {
      const { producto, noEncontrado } = this.estado();
      if (producto) {
        this.title.setTitle(`${producto.nombre} | BAN Sports`);
      } else if (noEncontrado) {
        this.title.setTitle('Producto no encontrado | BAN Sports');
      }
    });
  }

  /**
   * Elige un valor de un atributo. Si con lo ya elegido esa combinación no existe,
   * se pasa a la primera variante que tenga ese valor (preferentemente con stock).
   */
  elegir(clave: string, valor: string): void {
    const variantes = this.estado().variantes;
    const deseada = { ...this.seleccion(), [clave]: valor };
    const conValor = variantes.filter((variante) => variante.atributos[clave] === valor);
    const variante =
      variantes.find((v) => coincide(v, deseada)) ?? conValor.find((v) => v.stock > 0) ?? conValor[0];
    if (variante) {
      this.seleccion.set({ ...variante.atributos });
    }
  }

  cambiarCantidad(diferencia: number): void {
    this.cantidad.update((cantidad) => Math.min(Math.max(cantidad + diferencia, 1), Math.max(this.maximo(), 1)));
  }

  /**
   * Agrega la variante elegida al carrito. Sin sesión se guarda en el navegador
   * y se suma al de la cuenta cuando el cliente ingresa. Los admins no tienen carrito.
   */
  agregarAlCarrito(): void {
    const producto = this.estado().producto;
    const variante = this.varianteElegida();
    if (!producto || !variante || this.maximo() === 0 || this.authService.esAdmin()) {
      return;
    }

    this.agregando.set(true);
    this.resultadoAgregar.set(null);
    this.carritoService
      .agregar({ idProducto: producto.id, idVariante: variante.id, cantidad: this.cantidad() })
      .subscribe({
        next: () => {
          this.agregando.set(false);
          this.cantidad.set(1);
          this.resultadoAgregar.set({ ok: true, error: null });
        },
        error: (e: unknown) => {
          this.agregando.set(false);
          // El carrito pudo cambiar en otra pestaña: se trae de nuevo para que "Ya tenés N" quede al día.
          this.carritoService.cargar().subscribe({ error: () => {} });
          this.resultadoAgregar.set({
            ok: false,
            error:
              e instanceof HttpErrorResponse
                ? traducirErrorCarrito(e)
                : { titulo: 'No pudimos agregar el producto', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
          });
        }
      });
  }

  /** Texto de la variante para los mensajes: "1 kg · Chocolate". */
  protected descripcionVariante(variante: Variante): string {
    return atributosOrdenados(variante)
      .map(([, valor]) => valor)
      .join(' · ');
  }

  reintentar(): void {
    this.recargar$.next();
  }
}
