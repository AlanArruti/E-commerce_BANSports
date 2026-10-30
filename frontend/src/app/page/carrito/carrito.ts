import { Component, OnInit, computed, signal, ChangeDetectionStrategy } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { PoteProducto, abreviarNombre } from '../../componente/pote-producto/pote-producto';
import { ItemCarrito } from '../../models/item-carrito';
import { atributosOrdenados } from '../../models/variante';
import { PrecioPipe } from '../../pipes/precio.pipe';
import { CarritoService } from '../../services/carrito.service';
import { ErrorTraducido, traducirErrorCarrito } from '../../validaciones/errores-api';

const ERROR_INESPERADO: ErrorTraducido = {
  titulo: 'No pudimos actualizar el carrito',
  detalle: 'Ocurrió un error inesperado. Intentá de nuevo.'
};

@Component({
  selector: 'app-carrito',
  imports: [RouterLink, PoteProducto, PrecioPipe],
  templateUrl: './carrito.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './carrito.css'
})
export class Carrito implements OnInit {
  protected readonly cargando = signal(true);
  /** No se pudo traer el carrito al entrar: se muestra con "Reintentar". */
  protected readonly errorCarga = signal<ErrorTraducido | null>(null);
  /** Falló un cambio (cantidad, eliminar, vaciar): se muestra arriba de la lista. */
  protected readonly errorAccion = signal<ErrorTraducido | null>(null);
  /** Ítem que se está actualizando, para bloquear sus botones mientras tanto. */
  protected readonly itemPendiente = signal<number | null>(null);
  protected readonly confirmandoVaciar = signal(false);
  protected readonly vaciando = signal(false);

  protected readonly items;
  protected readonly cantidadTotal;

  constructor(protected carritoService: CarritoService) {
    this.items = computed(() => this.carritoService.carrito()?.items ?? []);
    this.cantidadTotal = this.carritoService.cantidadTotal;
  }

  ngOnInit(): void {
    this.cargar();
  }

  /** Siempre se pide de nuevo al entrar: el stock o los precios pueden haber cambiado. */
  cargar(): void {
    this.cargando.set(true);
    this.errorCarga.set(null);
    this.carritoService.cargar().subscribe({
      next: () => this.cargando.set(false),
      error: (e: unknown) => {
        this.cargando.set(false);
        this.errorCarga.set(e instanceof HttpErrorResponse ? traducirErrorCarrito(e) : ERROR_INESPERADO);
      }
    });
  }

  cambiarCantidad(item: ItemCarrito, diferencia: number): void {
    const cantidad = item.cantidad + diferencia;
    if (cantidad < 1 || cantidad > this.maximo(item)) {
      return;
    }
    this.ejecutar(item, this.carritoService.actualizarCantidad(item.idItemCarrito, cantidad));
  }

  eliminar(item: ItemCarrito): void {
    this.ejecutar(item, this.carritoService.eliminar(item.idItemCarrito));
  }

  vaciar(): void {
    this.vaciando.set(true);
    this.errorAccion.set(null);
    this.carritoService.vaciar().subscribe({
      next: () => {
        this.vaciando.set(false);
        this.confirmandoVaciar.set(false);
      },
      error: (e: unknown) => {
        this.vaciando.set(false);
        this.confirmandoVaciar.set(false);
        this.errorAccion.set(e instanceof HttpErrorResponse ? traducirErrorCarrito(e) : ERROR_INESPERADO);
      }
    });
  }

  /** Tope de unidades: el stock de la variante. Sin variante, el back no limita. */
  protected maximo(item: ItemCarrito): number {
    return item.variante?.stock ?? Number.MAX_SAFE_INTEGER;
  }

  /** "1 kg · Chocolate". Vacío si el producto no tiene variante. */
  protected descripcionVariante(item: ItemCarrito): string {
    return item.variante
      ? atributosOrdenados(item.variante)
          .map(([, valor]) => valor)
          .join(' · ')
      : '';
  }

  protected abreviatura(item: ItemCarrito): string {
    return abreviarNombre(item.producto.nombre);
  }

  private ejecutar(item: ItemCarrito, accion: Observable<unknown>): void {
    this.itemPendiente.set(item.idItemCarrito);
    this.errorAccion.set(null);
    accion.subscribe({
      next: () => this.itemPendiente.set(null),
      error: (e: unknown) => {
        this.itemPendiente.set(null);
        this.errorAccion.set(e instanceof HttpErrorResponse ? traducirErrorCarrito(e) : ERROR_INESPERADO);
      }
    });
  }
}
