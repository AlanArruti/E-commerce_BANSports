import { Injectable, computed, effect, signal, untracked } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import {
  Observable,
  catchError,
  concat,
  finalize,
  forkJoin,
  map,
  of,
  shareReplay,
  switchMap,
  tap,
  throwError,
  toArray
} from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiError } from '../models/api-error';
import { Carrito } from '../models/carrito';
import { ItemCarrito } from '../models/item-carrito';
import { ItemCarritoRequest } from '../models/item-carrito-request';
import { Pedido } from '../models/pedido';
import { Producto } from '../models/producto';
import { Variante, atributosOrdenados, precioFinalDe } from '../models/variante';
import { AuthService } from './auth.service';
import { ProductoService } from './producto.service';

/** Carrito de quien todavía no inició sesión. Se fusiona con el del back al ingresar. */
const CLAVE_LOCAL = 'ban_carrito_invitado';

/** Una línea del carrito sin sesión, tal como se guarda en localStorage. */
interface LineaLocal {
  idProducto: number;
  idVariante: number | null;
  cantidad: number;
  /** Último nombre conocido, para poder nombrarlo si el producto se da de baja. */
  nombre: string;
  detalle: string;
}

/** Producto del carrito sin sesión que se dio de baja (o su variante): hay que quitarlo para seguir. */
export interface ItemNoDisponible {
  indice: number;
  nombre: string;
  detalle: string;
}

/** Qué pasó al sumar el carrito sin sesión al de la cuenta. Se muestra en el carrito y el checkout. */
export interface ResultadoFusion {
  /** Unidades que se sumaron. */
  unidades: number;
  /** Productos que se sumaron con menos unidades por falta de stock. */
  ajustes: { nombre: string; querias: number; quedan: number }[];
  /** Productos que no se pudieron sumar (dados de baja o sin stock). */
  descartados: string[];
}

type ResultadoLinea =
  | { tipo: 'sumado'; linea: LineaLocal; unidades: number }
  | { tipo: 'ajustado'; linea: LineaLocal; unidades: number; querias: number; quedan: number }
  | { tipo: 'descartado'; linea: LineaLocal };

@Injectable({
  providedIn: 'root'
})
export class CarritoService {
  private readonly API_URL = environment.apiUrl;

  /** Carrito del cliente logueado o, sin sesión, el del navegador. null si todavía no se cargó. */
  readonly carrito = signal<Carrito | null>(null);

  /** Unidades en el carrito (no ítems): es lo que muestra el contador del header. */
  readonly cantidadTotal = computed(
    () => this.carrito()?.items.reduce((total, item) => total + item.cantidad, 0) ?? 0
  );

  /** Ítems del carrito sin sesión que ya no se pueden comprar. */
  readonly noDisponibles = signal<ItemNoDisponible[]>([]);

  /** Resultado de la última fusión, hasta que el cliente cierre el aviso o salga. */
  readonly avisoFusion = signal<ResultadoFusion | null>(null);

  /** Fusión en curso: mientras dura, cargar() espera a que termine en vez de pedir el carrito viejo. */
  private fusion$: Observable<Carrito> | null = null;

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private productoService: ProductoService
  ) {
    // El carrito sigue a la sesión. Este es el único punto "después del login": pasa igual
    // con el login normal, el registro o la vuelta de Google/GitHub, porque todos terminan
    // confirmando al cliente en authService.cliente().
    effect(() => {
      const cliente = this.authService.cliente();
      const logueado = this.authService.estaLogueado();
      untracked(() => {
        if (cliente) {
          // Si falla, el contador queda vacío; la página del carrito muestra el error al entrar.
          this.cargar().subscribe({ error: () => this.carrito.set(null) });
        } else if (!logueado) {
          this.avisoFusion.set(null);
          this.cargar().subscribe({ error: () => this.carrito.set(null) });
        } else {
          // Admin, o un cliente cuya ficha todavía no confirmó el back.
          this.carrito.set(null);
          this.noDisponibles.set([]);
        }
      });
    });
  }

  /** true si no hay sesión: el carrito vive en el navegador. */
  esInvitado(): boolean {
    return !this.authService.estaLogueado();
  }

  cargar(): Observable<Carrito> {
    if (this.esInvitado()) {
      return this.cargarLocal();
    }
    if (this.fusion$) {
      return this.fusion$;
    }
    if (this.leerLocal().length) {
      return this.fusionar();
    }
    return this.http.get<Carrito>(this.url()).pipe(tap((carrito) => this.carrito.set(carrito)));
  }

  /** Agrega un producto. Si ya estaba, se suma la cantidad (el back además valida que no supere el stock). */
  agregar(item: ItemCarritoRequest): Observable<Carrito> {
    if (this.esInvitado()) {
      const lineas = this.leerLocal();
      const existente = lineas.find(
        (linea) => linea.idProducto === item.idProducto && linea.idVariante === item.idVariante
      );
      if (existente) {
        existente.cantidad += item.cantidad;
      } else {
        lineas.push({ ...item, nombre: '', detalle: '' });
      }
      this.guardarLocal(lineas);
      return this.cargarLocal();
    }
    return this.http.post<Carrito>(`${this.url()}/items`, item).pipe(tap((carrito) => this.carrito.set(carrito)));
  }

  actualizarCantidad(idItem: number, cantidad: number): Observable<Carrito> {
    if (this.esInvitado()) {
      return this.cambiarLocal(idItem, (lineas, indice) => (lineas[indice].cantidad = cantidad));
    }
    return this.http
      .put<Carrito>(`${this.url()}/items/${idItem}`, { cantidad })
      .pipe(tap((carrito) => this.carrito.set(carrito)));
  }

  eliminar(idItem: number): Observable<Carrito> {
    if (this.esInvitado()) {
      return this.cambiarLocal(idItem, (lineas, indice) => lineas.splice(indice, 1));
    }
    return this.http
      .delete<Carrito>(`${this.url()}/items/${idItem}`)
      .pipe(tap((carrito) => this.carrito.set(carrito)));
  }

  /** Quita del carrito sin sesión un producto que ya no está disponible. */
  quitarNoDisponible(item: ItemNoDisponible): Observable<Carrito> {
    return this.cambiarLocal(item.indice + 1, (lineas, indice) => lineas.splice(indice, 1));
  }

  vaciar(): Observable<void> {
    if (this.esInvitado()) {
      this.guardarLocal([]);
      return this.cargarLocal().pipe(map(() => undefined));
    }
    return this.http.delete<void>(this.url()).pipe(
      tap(() => this.carrito.update((carrito) => (carrito ? { ...carrito, items: [], total: 0 } : carrito)))
    );
  }

  /**
   * Convierte el carrito en un pedido (queda en PENDIENTE_PAGO). El back valida el stock
   * y que la dirección sea del cliente, y vacía el carrito. Solo con sesión.
   */
  checkout(idDireccion: number): Observable<Pedido> {
    return this.http
      .post<Pedido>(`${this.url()}/checkout`, { idDireccion })
      .pipe(tap(() => this.carrito.update((carrito) => (carrito ? { ...carrito, items: [], total: 0 } : carrito))));
  }

  /** Cuántas unidades de esa variante ya hay en el carrito (para no ofrecer más que el stock). */
  cantidadDeVariante(idVariante: number): number {
    return this.carrito()?.items.find((item) => item.variante?.id === idVariante)?.cantidad ?? 0;
  }

  // ── Carrito sin sesión ─────────────────────────────────────────────

  /**
   * Arma el carrito del navegador con los datos actuales de cada producto (precio, stock).
   * Los productos dados de baja (el back responde 404) pasan a noDisponibles.
   */
  private cargarLocal(): Observable<Carrito> {
    const lineas = this.leerLocal();
    const ids = [...new Set(lineas.map((linea) => linea.idProducto))];
    const productos$ = ids.length
      ? forkJoin(
          ids.map((id) =>
            this.productoService
              .obtener(id)
              .pipe(
                catchError((e: unknown) =>
                  e instanceof HttpErrorResponse && e.status === 404 ? of(null) : throwError(() => e)
                )
              )
          )
        )
      : of([] as (Producto | null)[]);

    return productos$.pipe(
      map((productos) => {
        const porId = new Map(ids.map((id, i) => [id, productos[i]]));
        const items: ItemCarrito[] = [];
        const noDisponibles: ItemNoDisponible[] = [];

        lineas.forEach((linea, indice) => {
          const producto = porId.get(linea.idProducto) ?? null;
          const variante =
            linea.idVariante === null
              ? null
              : (producto?.variantes.find((v) => v.id === linea.idVariante && v.activo) ?? null);
          if (!producto || (linea.idVariante !== null && !variante)) {
            noDisponibles.push({ indice, nombre: linea.nombre || 'Producto', detalle: linea.detalle });
            return;
          }
          linea.nombre = producto.nombre;
          linea.detalle = variante ? describir(variante) : '';
          const precio = precioFinalDe(producto, variante);
          // El id es la posición en localStorage + 1: alcanza para cambiar o quitar ese ítem.
          items.push({ idItemCarrito: indice + 1, producto, variante, cantidad: linea.cantidad, subtotal: precio * linea.cantidad });
        });

        this.guardarLocal(lineas);
        const carrito: Carrito = {
          id: 0,
          uuidCarrito: '',
          items,
          total: items.reduce((total, item) => total + item.subtotal, 0)
        };
        this.carrito.set(carrito);
        this.noDisponibles.set(noDisponibles);
        return carrito;
      })
    );
  }

  private cambiarLocal(idItem: number, cambio: (lineas: LineaLocal[], indice: number) => void): Observable<Carrito> {
    const lineas = this.leerLocal();
    const indice = idItem - 1;
    if (lineas[indice]) {
      cambio(lineas, indice);
      this.guardarLocal(lineas);
    }
    return this.cargarLocal();
  }

  // ── Fusión al iniciar sesión ───────────────────────────────────────

  /**
   * Suma el carrito del navegador al de la cuenta, de a un producto. Si no alcanza el stock,
   * agrega lo que queda; si el producto ya no existe, lo descarta. Al terminar borra el local.
   * Si falla antes de empezar (no se pudo leer el carrito del back), el local se conserva.
   */
  private fusionar(): Observable<Carrito> {
    const lineas = this.leerLocal();
    this.fusion$ = this.http.get<Carrito>(this.url()).pipe(
      switchMap((delBack) => concat(...lineas.map((linea) => this.sumarAlBack(linea, delBack))).pipe(toArray())),
      switchMap((resultados) => {
        this.guardarLocal([]);
        this.noDisponibles.set([]);
        this.avisoFusion.set(resumirFusion(resultados));
        return this.http.get<Carrito>(this.url());
      }),
      tap((carrito) => this.carrito.set(carrito)),
      finalize(() => (this.fusion$ = null)),
      shareReplay(1)
    );
    return this.fusion$;
  }

  private sumarAlBack(linea: LineaLocal, delBack: Carrito): Observable<ResultadoLinea> {
    const pedido: ItemCarritoRequest = { idProducto: linea.idProducto, idVariante: linea.idVariante, cantidad: linea.cantidad };
    return this.http.post<Carrito>(`${this.url()}/items`, pedido).pipe(
      map((): ResultadoLinea => ({ tipo: 'sumado', linea, unidades: linea.cantidad })),
      catchError((e: unknown) => {
        // "Stock insuficiente para la variante id 3. Disponible: 25, solicitado: 26."
        const mensaje = e instanceof HttpErrorResponse ? ((e.error as Partial<ApiError> | null)?.message ?? '') : '';
        const disponible = Number(/Disponible:\s*(\d+)/.exec(mensaje)?.[1]);
        if (!(e instanceof HttpErrorResponse) || e.status !== 400 || !Number.isInteger(disponible)) {
          return of<ResultadoLinea>({ tipo: 'descartado', linea });
        }
        const yaEnLaCuenta = delBack.items.find((item) => item.variante?.id === linea.idVariante)?.cantidad ?? 0;
        const posible = disponible - yaEnLaCuenta;
        const ajuste: ResultadoLinea = {
          tipo: 'ajustado',
          linea,
          unidades: Math.max(posible, 0),
          querias: yaEnLaCuenta + linea.cantidad,
          quedan: disponible
        };
        if (posible <= 0) {
          return of(ajuste);
        }
        return this.http.post<Carrito>(`${this.url()}/items`, { ...pedido, cantidad: posible }).pipe(
          map(() => ajuste),
          catchError(() => of<ResultadoLinea>({ tipo: 'descartado', linea }))
        );
      })
    );
  }

  // ── localStorage ───────────────────────────────────────────────────
  // Puede no estar disponible (modo privado, almacenamiento bloqueado) o tener datos rotos:
  // en esos casos el carrito arranca vacío.

  private leerLocal(): LineaLocal[] {
    try {
      const datos: unknown = JSON.parse(localStorage.getItem(CLAVE_LOCAL) ?? '[]');
      return Array.isArray(datos) ? datos.filter(esLineaValida) : [];
    } catch {
      return [];
    }
  }

  private guardarLocal(lineas: LineaLocal[]): void {
    try {
      if (lineas.length) {
        localStorage.setItem(CLAVE_LOCAL, JSON.stringify(lineas));
      } else {
        localStorage.removeItem(CLAVE_LOCAL);
      }
    } catch {
      // Sin almacenamiento el carrito dura lo que dure la página.
    }
  }

  private url(): string {
    return `${this.API_URL}/cliente/${this.authService.cliente()?.id}/carrito`;
  }
}

function esLineaValida(dato: unknown): dato is LineaLocal {
  const linea = dato as Partial<LineaLocal> | null;
  return (
    typeof linea === 'object' &&
    linea !== null &&
    Number.isInteger(linea.idProducto) &&
    (linea.idVariante === null || Number.isInteger(linea.idVariante)) &&
    Number.isInteger(linea.cantidad) &&
    (linea.cantidad ?? 0) > 0 &&
    typeof linea.nombre === 'string' &&
    typeof linea.detalle === 'string'
  );
}

/** "1 kg · Chocolate" */
function describir(variante: Variante): string {
  return atributosOrdenados(variante)
    .map(([, valor]) => valor)
    .join(' · ');
}

function resumirFusion(resultados: ResultadoLinea[]): ResultadoFusion {
  const nombre = (linea: LineaLocal) => [linea.nombre || 'Producto', linea.detalle].filter(Boolean).join(' ');
  return {
    unidades: resultados.reduce((total, r) => total + (r.tipo === 'descartado' ? 0 : r.unidades), 0),
    ajustes: resultados.flatMap((r) =>
      r.tipo === 'ajustado' ? [{ nombre: nombre(r.linea), querias: r.querias, quedan: r.quedan }] : []
    ),
    descartados: resultados.flatMap((r) => (r.tipo === 'descartado' ? [nombre(r.linea)] : []))
  };
}
