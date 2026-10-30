import { Component, OnInit, computed, signal, ChangeDetectionStrategy } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { FormDireccion } from '../../componente/form-direccion/form-direccion';
import { Direccion, localidadDeDireccion } from '../../models/direccion';
import { ItemCarrito } from '../../models/item-carrito';
import { ETIQUETAS_ESTADO_PEDIDO, Pedido } from '../../models/pedido';
import { atributosOrdenados } from '../../models/variante';
import { PrecioPipe } from '../../pipes/precio.pipe';
import { CarritoService } from '../../services/carrito.service';
import { DireccionService } from '../../services/direccion.service';
import { ErrorTraducido, traducirErrorCarrito, traducirErrorCheckout } from '../../validaciones/errores-api';

/** Checkout: elegir (o cargar) la dirección de entrega y convertir el carrito en un pedido. */
@Component({
  selector: 'app-checkout',
  imports: [NgClass, DatePipe, RouterLink, PrecioPipe, FormDireccion],
  templateUrl: './checkout.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './checkout.css'
})
export class Checkout implements OnInit {
  protected readonly cargando = signal(true);
  protected readonly errorCarga = signal<ErrorTraducido | null>(null);
  protected readonly direcciones = signal<Direccion[]>([]);
  protected readonly idDireccionElegida = signal<number | null>(null);
  /** Formulario de dirección abierto. Siempre lo está si el cliente no tiene ninguna. */
  protected readonly agregandoDireccion = signal(false);
  protected readonly confirmando = signal(false);
  protected readonly errorConfirmar = signal<ErrorTraducido | null>(null);
  /** Pedido recién creado: se muestra la pantalla de confirmación. */
  protected readonly pedido = signal<Pedido | null>(null);
  protected readonly direccionDelPedido = signal<Direccion | null>(null);

  protected readonly items;
  protected readonly direccionElegida;
  protected readonly unidadesDelPedido;
  protected readonly etiquetasEstado = ETIQUETAS_ESTADO_PEDIDO;
  protected readonly localidad = localidadDeDireccion;

  constructor(
    protected carritoService: CarritoService,
    private direccionService: DireccionService,
    private title: Title
  ) {
    this.items = computed(() => this.carritoService.carrito()?.items ?? []);
    this.direccionElegida = computed(
      () => this.direcciones().find((direccion) => direccion.id === this.idDireccionElegida()) ?? null
    );
    this.unidadesDelPedido = computed(
      () => this.pedido()?.items.reduce((total, item) => total + item.cantidad, 0) ?? 0
    );
  }

  ngOnInit(): void {
    this.cargar();
  }

  /** Trae el carrito actualizado (stock y precios) y las direcciones del cliente. */
  cargar(): void {
    this.cargando.set(true);
    this.errorCarga.set(null);
    forkJoin([this.carritoService.cargar(), this.direccionService.listar()]).subscribe({
      next: ([, direcciones]) => {
        this.cargando.set(false);
        this.direcciones.set(direcciones);
        this.idDireccionElegida.set(direcciones[0]?.id ?? null);
        this.agregandoDireccion.set(direcciones.length === 0);
      },
      error: (e: unknown) => {
        this.cargando.set(false);
        this.errorCarga.set(
          e instanceof HttpErrorResponse
            ? traducirErrorCarrito(e)
            : { titulo: 'No pudimos cargar tu pedido', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
      }
    });
  }

  elegirDireccion(id: number): void {
    this.idDireccionElegida.set(id);
    this.errorConfirmar.set(null);
  }

  /** La dirección nueva se suma a la lista sin recargar y queda elegida. */
  direccionGuardada(direccion: Direccion): void {
    this.direcciones.update((direcciones) => [...direcciones, direccion]);
    this.idDireccionElegida.set(direccion.id);
    this.agregandoDireccion.set(false);
    this.errorConfirmar.set(null);
  }

  confirmar(): void {
    const direccion = this.direccionElegida();
    if (!direccion) {
      this.errorConfirmar.set({
        titulo: 'Falta la dirección de entrega',
        detalle: 'Cargá una dirección para que sepamos a dónde enviar tu pedido.'
      });
      return;
    }

    this.confirmando.set(true);
    this.errorConfirmar.set(null);
    this.carritoService.checkout(direccion.id).subscribe({
      next: (pedido) => {
        this.confirmando.set(false);
        this.direccionDelPedido.set(direccion);
        this.pedido.set(pedido);
        this.title.setTitle(`Pedido #${pedido.id} confirmado | BAN Sports`);
        window.scrollTo({ top: 0 });
      },
      error: (e: unknown) => {
        this.confirmando.set(false);
        this.errorConfirmar.set(
          e instanceof HttpErrorResponse
            ? traducirErrorCheckout(e)
            : { titulo: 'No pudimos confirmar el pedido', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
      }
    });
  }

  /** "1 kg · Chocolate". Vacío si el producto no tiene variante. */
  protected descripcionVariante(item: ItemCarrito): string {
    return item.variante
      ? atributosOrdenados(item.variante)
          .map(([, valor]) => valor)
          .join(' · ')
      : '';
  }
}
