import { Component, computed, effect, signal, ChangeDetectionStrategy } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, map, of, startWith, switchMap } from 'rxjs';
import { EstadoPedidoEtiqueta } from '../../componente/estado-pedido/estado-pedido';
import { PoteProducto, abreviarNombre } from '../../componente/pote-producto/pote-producto';
import { localidadDeDireccion } from '../../models/direccion';
import {
  ETIQUETAS_ESTADO_PEDIDO,
  Factura,
  ItemPedido,
  PASOS_PEDIDO,
  Pedido,
  clientePuedeCancelar,
  numeroFactura
} from '../../models/pedido';
import { atributosOrdenados } from '../../models/variante';
import { PrecioPipe } from '../../pipes/precio.pipe';
import { ClienteService } from '../../services/cliente.service';
import { ErrorTraducido, traducirErrorCancelarPedido, traducirErrorMisPedidos } from '../../validaciones/errores-api';

interface EstadoDetalle {
  cargando: boolean;
  pedido: Pedido | null;
  noEncontrado: boolean;
  error: ErrorTraducido | null;
}

interface PasoAvance {
  etiqueta: string;
  hecho: boolean;
  actual: boolean;
}

const CARGANDO: EstadoDetalle = { cargando: true, pedido: null, noEncontrado: false, error: null };

/**
 * Detalle de un pedido del cliente: avance, ítems, dirección y total (SCRUM-121).
 * Seguimiento del envío, factura y cancelación (SCRUM-140).
 */
@Component({
  selector: 'app-detalle-pedido',
  imports: [DatePipe, RouterLink, EstadoPedidoEtiqueta, PoteProducto, PrecioPipe],
  templateUrl: './detalle-pedido.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './detalle-pedido.css',
  host: { '(document:keydown.escape)': 'cerrarCancelacion()' }
})
export class DetallePedido {
  protected readonly estado;
  protected readonly pasos;
  protected readonly unidades;
  protected readonly localidad = localidadDeDireccion;
  protected readonly numero = (factura: Factura) => numeroFactura(factura);
  protected readonly puedeCancelar = (pedido: Pedido) => clientePuedeCancelar(pedido.estado);

  protected readonly aviso = signal<string | null>(null);
  protected readonly confirmando = signal(false);
  protected readonly cancelando = signal(false);
  protected readonly errorCancelar = signal<ErrorTraducido | null>(null);
  /** "¡Copiado!" en el botón del número de seguimiento, por un momento. */
  protected readonly copiado = signal(false);

  /** Emite para volver a pedirlo (botón "Reintentar"). */
  private readonly recargar$ = new BehaviorSubject<void>(undefined);

  constructor(
    private clienteService: ClienteService,
    route: ActivatedRoute,
    title: Title
  ) {
    const id$ = route.paramMap.pipe(map((params) => Number(params.get('id'))));

    // Un pedido de otro cliente (403) se muestra igual que uno que no existe: no se revela que existe.
    // Al recargar después de cancelar no se vuelve al esqueleto: se queda lo que había hasta que llega lo nuevo.
    let anterior: EstadoDetalle = CARGANDO;
    this.estado = toSignal(
      combineLatest([id$, this.recargar$]).pipe(
        switchMap(([id]) => {
          if (!Number.isInteger(id) || id <= 0) {
            return of<EstadoDetalle>({ ...CARGANDO, cargando: false, noEncontrado: true });
          }
          return this.clienteService.obtenerPedido(id).pipe(
            map((pedido): EstadoDetalle => ({ ...CARGANDO, cargando: false, pedido })),
            catchError((e: unknown) => {
              if (e instanceof HttpErrorResponse && (e.status === 403 || e.status === 404)) {
                return of<EstadoDetalle>({ ...CARGANDO, cargando: false, noEncontrado: true });
              }
              return of<EstadoDetalle>({
                ...CARGANDO,
                cargando: false,
                error:
                  e instanceof HttpErrorResponse
                    ? traducirErrorMisPedidos(e)
                    : { titulo: 'No pudimos cargar el pedido', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
              });
            }),
            startWith(anterior.pedido?.id === id ? anterior : CARGANDO)
          );
        }),
        map((estado) => (anterior = estado))
      ),
      { initialValue: CARGANDO }
    );

    // Barra de avance: Pendiente de pago → Pagado → En preparación → Despachado → Entregado.
    this.pasos = computed<PasoAvance[]>(() => {
      const pedido = this.estado().pedido;
      const actual = pedido ? PASOS_PEDIDO.indexOf(pedido.estado) : -1;
      return PASOS_PEDIDO.map((paso, i) => ({
        etiqueta: ETIQUETAS_ESTADO_PEDIDO[paso],
        hecho: i <= actual,
        actual: i === actual
      }));
    });

    this.unidades = computed(
      () => this.estado().pedido?.items.reduce((total, item) => total + item.cantidad, 0) ?? 0
    );

    effect(() => {
      const { pedido, noEncontrado } = this.estado();
      if (pedido) {
        title.setTitle(`Pedido #${pedido.id} | BAN Sports`);
      } else if (noEncontrado) {
        title.setTitle('Pedido no encontrado | BAN Sports');
      }
    });
  }

  reintentar(): void {
    this.recargar$.next();
  }

  pedirCancelacion(): void {
    this.aviso.set(null);
    this.errorCancelar.set(null);
    this.confirmando.set(true);
  }

  cerrarCancelacion(): void {
    if (!this.cancelando()) {
      this.confirmando.set(false);
    }
  }

  confirmarCancelacion(pedido: Pedido): void {
    this.cancelando.set(true);
    this.clienteService.cancelarPedido(pedido.id).subscribe({
      next: () => {
        this.cancelando.set(false);
        this.confirmando.set(false);
        this.aviso.set(`Cancelaste el pedido #${pedido.id}.`);
        this.recargar$.next();
      },
      error: (e: unknown) => {
        this.cancelando.set(false);
        this.confirmando.set(false);
        this.errorCancelar.set(
          e instanceof HttpErrorResponse
            ? traducirErrorCancelarPedido(e)
            : { titulo: 'No pudimos cancelar el pedido', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
        // Si cambió de estado (por ejemplo, se acaba de pagar), que se vea el estado real.
        this.recargar$.next();
      }
    });
  }

  copiarSeguimiento(numero: string): void {
    navigator.clipboard?.writeText(numero).then(
      () => {
        this.copiado.set(true);
        setTimeout(() => this.copiado.set(false), 2000);
      },
      () => undefined
    );
  }

  /** "1 kg · Chocolate". Vacío si el producto no tiene variante. */
  protected descripcionVariante(item: ItemPedido): string {
    return item.variante
      ? atributosOrdenados(item.variante)
          .map(([, valor]) => valor)
          .join(' · ')
      : '';
  }

  /** El back guarda el total de la línea; el unitario es el precio al momento de la compra. */
  protected precioUnitario(item: ItemPedido): number {
    return item.precioTotalProducto / item.cantidad;
  }

  protected abreviatura(item: ItemPedido): string {
    return abreviarNombre(item.producto.nombre);
  }
}
