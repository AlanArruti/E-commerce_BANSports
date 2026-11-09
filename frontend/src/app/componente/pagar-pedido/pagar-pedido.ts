import { ChangeDetectionStrategy, Component, OnInit, input, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Pasarela, PagoService, TipoFactura } from '../../services/pago.service';
import { ErrorTraducido, traducirErrorPago } from '../../validaciones/errores-api';

/**
 * Pagar un pedido pendiente (SCRUM-122): elige el tipo de factura y la pasarela, y lleva
 * al cliente a pagar. Muestra un botón por cada pasarela disponible, así que sumar una
 * pasarela en el back no requiere cambios acá.
 * Se usa en la confirmación del checkout, en la pantalla de vuelta y se puede usar en Mis pedidos.
 */
@Component({
  selector: 'app-pagar-pedido',
  templateUrl: './pagar-pedido.html',
  styleUrl: './pagar-pedido.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PagarPedido implements OnInit {
  readonly idPedido = input.required<number>();
  /** Texto del botón; "{pasarela}" se reemplaza por su nombre. */
  readonly textoBoton = input('Pagar con {pasarela}');

  protected readonly pasarelas = signal<Pasarela[] | null>(null);
  protected readonly tipoFactura = signal<TipoFactura>('B');
  protected readonly redirigiendo = signal<string | null>(null);
  protected readonly error = signal<ErrorTraducido | null>(null);

  protected readonly opciones: { valor: TipoFactura; titulo: string; detalle: string }[] = [
    { valor: 'B', titulo: 'Factura B', detalle: 'Consumidor final' },
    { valor: 'A', titulo: 'Factura A', detalle: 'Responsable inscripto' }
  ];

  constructor(private pagoService: PagoService) {}

  ngOnInit(): void {
    this.pagoService.pasarelas().subscribe((lista) => this.pasarelas.set(lista));
  }

  protected texto(pasarela: Pasarela): string {
    return this.textoBoton().replace('{pasarela}', pasarela.nombre);
  }

  pagar(pasarela: Pasarela): void {
    this.error.set(null);
    this.redirigiendo.set(pasarela.codigo);
    this.pagoService.iniciarPago(this.idPedido(), pasarela.codigo, this.tipoFactura()).subscribe({
      next: ({ urlPago }) => {
        // Sale de la app hacia la pasarela; la vuelta llega a /pago/resultado?pago=...
        window.location.href = urlPago;
      },
      error: (e: HttpErrorResponse) => {
        this.redirigiendo.set(null);
        this.error.set(traducirErrorPago(e));
      }
    });
  }
}
