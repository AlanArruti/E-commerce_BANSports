import { Component, OnDestroy, OnInit, computed, signal, ChangeDetectionStrategy } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { TarjetaProducto } from '../../componente/tarjeta-producto/tarjeta-producto';
import { Producto } from '../../models/producto';
import { Variante } from '../../models/variante';
import { CarritoService } from '../../services/carrito.service';
import { FavoritoService } from '../../services/favorito.service';
import { ErrorTraducido, traducirErrorCarrito, traducirErrorFavoritos } from '../../validaciones/errores-api';
import { ErrorCarga } from '../../componente/error-carga/error-carga';

const ERROR_INESPERADO: ErrorTraducido = {
  titulo: 'No pudimos cargar tus favoritos',
  detalle: 'Ocurrió un error inesperado. Intentá de nuevo.'
};

/**
 * Qué botón lleva cada favorito:
 * - agregar: una sola variante con stock (o ninguna cargada), va directo al carrito.
 * - elegir: varias variantes, hay que elegir en el detalle cuál se compra.
 * - sin-stock: ninguna variante tiene stock.
 */
type Accion =
  | { tipo: 'agregar'; variante: Variante | null }
  | { tipo: 'elegir'; etiqueta: string }
  | { tipo: 'sin-stock' };

@Component({
  selector: 'app-favoritos',
  imports: [ErrorCarga, RouterLink, TarjetaProducto],
  templateUrl: './favoritos.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './favoritos.css'
})
export class Favoritos implements OnInit, OnDestroy {
  protected readonly cargando = signal(true);
  protected readonly errorCarga = signal<ErrorTraducido | null>(null);
  /** Producto que se está agregando al carrito, para bloquear su botón. */
  protected readonly agregando = signal<number | null>(null);
  /** Productos agregados al carrito desde esta página, para mostrar "Agregado". */
  protected readonly agregados = signal<ReadonlySet<number>>(new Set());
  protected readonly errorCarrito = signal<ErrorTraducido | null>(null);

  protected readonly items = computed(() =>
    (this.favoritoService.favoritos() ?? []).map((favorito) => ({
      producto: favorito.producto,
      accion: this.accion(favorito.producto)
    }))
  );

  constructor(
    protected favoritoService: FavoritoService,
    private carritoService: CarritoService
  ) {}

  ngOnInit(): void {
    this.favoritoService.ultimoQuitado.set(null);
    this.favoritoService.errorAccion.set(null);
    this.cargar();
  }

  ngOnDestroy(): void {
    // El "Deshacer" es solo de esta página.
    this.favoritoService.ultimoQuitado.set(null);
  }

  /** Siempre se pide de nuevo al entrar: un producto pudo darse de baja o quedarse sin stock. */
  cargar(): void {
    this.cargando.set(true);
    this.errorCarga.set(null);
    this.favoritoService.cargar().subscribe({
      next: () => this.cargando.set(false),
      error: (e: unknown) => {
        this.cargando.set(false);
        this.errorCarga.set(e instanceof HttpErrorResponse ? traducirErrorFavoritos(e) : ERROR_INESPERADO);
      }
    });
  }

  agregarAlCarrito(producto: Producto, variante: Variante | null): void {
    this.agregando.set(producto.id);
    this.errorCarrito.set(null);
    this.carritoService.agregar({ idProducto: producto.id, idVariante: variante?.id ?? null, cantidad: 1 }).subscribe({
      next: () => {
        this.agregando.set(null);
        this.agregados.update((agregados) => new Set(agregados).add(producto.id));
      },
      error: (e: unknown) => {
        this.agregando.set(null);
        this.errorCarrito.set(
          e instanceof HttpErrorResponse
            ? traducirErrorCarrito(e)
            : { titulo: 'No pudimos agregar el producto', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
      }
    });
  }

  /** Vuelve a guardar el último producto que se quitó con el corazón. */
  deshacer(): void {
    const producto = this.favoritoService.ultimoQuitado();
    if (producto) {
      this.favoritoService.alternar(producto);
    }
  }

  private accion(producto: Producto): Accion {
    const activas = producto.variantes.filter((variante) => variante.activo);
    if (!activas.length) {
      return { tipo: 'agregar', variante: null };
    }
    if (!activas.some((variante) => variante.stock > 0)) {
      return { tipo: 'sin-stock' };
    }
    if (activas.length === 1) {
      return { tipo: 'agregar', variante: activas[0] };
    }
    const tieneSabor = activas.some((variante) => 'sabor' in variante.atributos);
    return { tipo: 'elegir', etiqueta: tieneSabor ? 'Elegir sabor' : 'Elegir opción' };
  }
}
