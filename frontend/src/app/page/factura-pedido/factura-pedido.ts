import { Component, computed, effect, ChangeDetectionStrategy } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, combineLatest, map, of, startWith, switchMap } from 'rxjs';
import { NoEncontrado } from '../../componente/no-encontrado/no-encontrado';
import { localidadDeDireccion } from '../../models/direccion';
import { ItemPedido, Pedido, numeroFactura } from '../../models/pedido';
import { atributosOrdenados } from '../../models/variante';
import { PrecioPipe } from '../../pipes/precio.pipe';
import { ClienteService } from '../../services/cliente.service';
import { ErrorTraducido, traducirErrorPedidos } from '../../validaciones/errores-api';

interface EstadoFactura {
  cargando: boolean;
  pedido: Pedido | null;
  noEncontrado: boolean;
  error: ErrorTraducido | null;
}

const CARGANDO: EstadoFactura = { cargando: true, pedido: null, noEncontrado: false, error: null };

/**
 * Factura de un pedido del cliente, lista para imprimir (SCRUM-140).
 * Se arma con los datos del pedido: el back solo guarda tipo, total y fecha de la factura.
 */
@Component({
  selector: 'app-factura-pedido',
  imports: [DatePipe, RouterLink, NoEncontrado, PrecioPipe],
  templateUrl: './factura-pedido.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './factura-pedido.css'
})
export class FacturaPedido {
  protected readonly estado;
  protected readonly numero;
  protected readonly localidad = localidadDeDireccion;

  private readonly recargar$ = new BehaviorSubject<void>(undefined);

  constructor(
    private clienteService: ClienteService,
    route: ActivatedRoute,
    title: Title
  ) {
    const id$ = route.paramMap.pipe(map((params) => Number(params.get('id'))));

    // Un pedido de otro cliente (403) se muestra igual que uno que no existe.
    this.estado = toSignal(
      combineLatest([id$, this.recargar$]).pipe(
        switchMap(([id]) => {
          if (!Number.isInteger(id) || id <= 0) {
            return of<EstadoFactura>({ ...CARGANDO, cargando: false, noEncontrado: true });
          }
          return this.clienteService.obtenerPedido(id).pipe(
            map((pedido): EstadoFactura => ({ ...CARGANDO, cargando: false, pedido })),
            catchError((e: unknown) => {
              if (e instanceof HttpErrorResponse && (e.status === 403 || e.status === 404)) {
                return of<EstadoFactura>({ ...CARGANDO, cargando: false, noEncontrado: true });
              }
              return of<EstadoFactura>({
                ...CARGANDO,
                cargando: false,
                error:
                  e instanceof HttpErrorResponse
                    ? traducirErrorPedidos(e)
                    : { titulo: 'No pudimos cargar la factura', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
              });
            }),
            startWith(CARGANDO)
          );
        })
      ),
      { initialValue: CARGANDO }
    );

    this.numero = computed(() => {
      const factura = this.estado().pedido?.factura;
      return factura ? numeroFactura(factura) : '';
    });

    effect(() => {
      const { pedido, noEncontrado } = this.estado();
      if (pedido?.factura) {
        title.setTitle(`Factura ${pedido.factura.tipo} ${this.numero()} | BAN Sports`);
      } else if (pedido || noEncontrado) {
        title.setTitle('Factura no encontrada | BAN Sports');
      }
    });
  }

  reintentar(): void {
    this.recargar$.next();
  }

  imprimir(): void {
    window.print();
  }

  /** "Whey Protein · 1 kg · Chocolate" */
  protected descripcion(item: ItemPedido): string {
    const variante = item.variante
      ? atributosOrdenados(item.variante)
          .map(([, valor]) => valor)
          .join(' · ')
      : '';
    return variante ? `${item.producto.nombre} · ${variante}` : item.producto.nombre;
  }

  protected precioUnitario(item: ItemPedido): number {
    return item.precioTotalProducto / item.cantidad;
  }
}
