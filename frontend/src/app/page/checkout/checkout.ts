import { Component, OnInit, computed, signal, ChangeDetectionStrategy } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { AvisoFusion } from '../../componente/aviso-fusion/aviso-fusion';
import { forkJoin } from 'rxjs';
import { FormDireccion } from '../../componente/form-direccion/form-direccion';
import { Direccion, localidadDeDireccion } from '../../models/direccion';
import { ItemCarrito } from '../../models/item-carrito';
import { ETIQUETAS_ESTADO_PEDIDO, Pedido } from '../../models/pedido';
import { CotizacionEnvio, plazoDeEnvio } from '../../models/cotizacion-envio';
import { atributosOrdenados } from '../../models/variante';
import { PrecioPipe } from '../../pipes/precio.pipe';
import { CarritoService } from '../../services/carrito.service';
import { CotizacionService } from '../../services/cotizacion.service';
import { DireccionService } from '../../services/direccion.service';
import { ErrorTraducido, traducirErrorCarrito, traducirErrorCheckout, traducirErrorCotizacion } from '../../validaciones/errores-api';
import { ErrorCarga } from '../../componente/error-carga/error-carga';

/** Checkout: elegir (o cargar) la dirección de entrega y convertir el carrito en un pedido. */
@Component({
  selector: 'app-checkout',
  imports: [ErrorCarga, NgClass, DatePipe, RouterLink, PrecioPipe, FormDireccion, AvisoFusion],
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

  /** Opciones de envío de la dirección elegida. */
  protected readonly cotizaciones = signal<CotizacionEnvio[]>([]);
  protected readonly idOpcionElegida = signal<number | null>(null);
  protected readonly cargandoEnvio = signal(false);
  protected readonly errorEnvio = signal<ErrorTraducido | null>(null);

  protected readonly items;
  protected readonly direccionElegida;
  protected readonly opcionElegida;
  protected readonly costoEnvio;
  protected readonly totalConEnvio;
  protected readonly unidadesDelPedido;
  protected readonly etiquetasEstado = ETIQUETAS_ESTADO_PEDIDO;
  protected readonly localidad = localidadDeDireccion;
  protected readonly plazoDeEnvio = plazoDeEnvio;

  constructor(
    protected carritoService: CarritoService,
    private direccionService: DireccionService,
    private cotizacionService: CotizacionService,
    private title: Title
  ) {
    this.items = computed(() => this.carritoService.carrito()?.items ?? []);
    this.direccionElegida = computed(
      () => this.direcciones().find((direccion) => direccion.id === this.idDireccionElegida()) ?? null
    );
    this.opcionElegida = computed(() => {
      const indice = this.idOpcionElegida();
      return indice === null ? null : (this.cotizaciones()[indice] ?? null);
    });
    this.costoEnvio = computed(() => this.opcionElegida()?.precio ?? 0);
    this.totalConEnvio = computed(() => (this.carritoService.carrito()?.total ?? 0) + this.costoEnvio());
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
        this.cargarCotizacion(direcciones[0]?.id ?? null);
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

  /** Cotiza el envío para la dirección dada. Si falla, muestra el aviso sin trabar el checkout. */
  cargarCotizacion(idDireccion: number | null): void {
    if (idDireccion === null) {
      this.cotizaciones.set([]);
      this.idOpcionElegida.set(null);
      this.errorEnvio.set(null);
      return;
    }

    this.cargandoEnvio.set(true);
    this.errorEnvio.set(null);
    this.cotizacionService.cotizar(idDireccion).subscribe({
      next: (opciones) => {
        this.cargandoEnvio.set(false);
        this.cotizaciones.set(opciones);
        this.idOpcionElegida.set(opciones.length ? indiceMasBarata(opciones) : null);
      },
      error: (e: unknown) => {
        this.cargandoEnvio.set(false);
        this.cotizaciones.set([]);
        this.idOpcionElegida.set(null);
        this.errorEnvio.set(
          e instanceof HttpErrorResponse
            ? traducirErrorCotizacion(e)
            : { titulo: 'No pudimos calcular el envío', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
      }
    });
  }

  elegirDireccion(id: number): void {
    this.idDireccionElegida.set(id);
    this.errorConfirmar.set(null);
    this.cargarCotizacion(id);
  }

  elegirOpcion(indice: number): void {
    this.idOpcionElegida.set(indice);
    this.errorConfirmar.set(null);
  }

  /** La dirección nueva se suma a la lista sin recargar y queda elegida. */
  direccionGuardada(direccion: Direccion): void {
    this.direcciones.update((direcciones) => [...direcciones, direccion]);
    this.idDireccionElegida.set(direccion.id);
    this.agregandoDireccion.set(false);
    this.errorConfirmar.set(null);
    this.cargarCotizacion(direccion.id);
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

    const opcion = this.opcionElegida();
    if (!opcion) {
      this.errorConfirmar.set({
        titulo: 'Falta el costo de envío',
        detalle: 'Elegí una opción de envío para confirmar el pedido.'
      });
      return;
    }

    this.confirmando.set(true);
    this.errorConfirmar.set(null);
    this.carritoService.checkout(direccion.id, opcion.precio).subscribe({
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

/** Índice de la opción más barata. */
function indiceMasBarata(opciones: CotizacionEnvio[]): number {
  let indice = 0;
  for (let i = 1; i < opciones.length; i++) {
    if (opciones[i].precio < opciones[indice].precio) {
      indice = i;
    }
  }
  return indice;
}
