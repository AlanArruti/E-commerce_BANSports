import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PagarPedido } from '../../componente/pagar-pedido/pagar-pedido';
import { PrecioPipe } from '../../pipes/precio.pipe';
import { Pago, PagoService } from '../../services/pago.service';
import { ErrorTraducido, traducirErrorPago } from '../../validaciones/errores-api';

type Resultado = 'cargando' | 'pagado' | 'procesando' | 'rechazado' | 'sin-pago' | 'error';

/**
 * Vuelta de cualquier pasarela de pago (SCRUM-122): /pago/resultado?pago={id}.
 * El estado sale del back, que consulta a la pasarela. La URL no se usa como prueba de pago,
 * y la pantalla no sabe de qué pasarela vuelve el cliente.
 */
@Component({
  selector: 'app-resultado-pago',
  imports: [RouterLink, PrecioPipe, PagarPedido],
  templateUrl: './resultado-pago.html',
  styleUrl: './resultado-pago.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ResultadoPago implements OnInit {
  protected readonly resultado = signal<Resultado>('cargando');
  protected readonly pago = signal<Pago | null>(null);
  protected readonly error = signal<ErrorTraducido | null>(null);

  protected readonly hayIdDePago = signal(false);
  private idPago: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private pagoService: PagoService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.queryParamMap.get('pago'));
    if (!Number.isInteger(id) || id <= 0) {
      this.mostrarError({ titulo: 'No sabemos qué pago hiciste', detalle: 'Buscá el pedido en Mis pedidos para ver su estado.' });
      return;
    }
    this.idPago = id;
    this.hayIdDePago.set(true);
    this.consultar();
  }

  /** Vuelve a preguntarle al back (por ejemplo, si el pago todavía se estaba procesando). */
  consultar(): void {
    if (this.idPago === null) {
      return;
    }
    this.resultado.set('cargando');
    this.error.set(null);

    this.pagoService.confirmarPago(this.idPago).subscribe({
      next: (pago) => {
        this.pago.set(pago);
        this.resultado.set(this.interpretar(pago));
      },
      error: (e: HttpErrorResponse) => this.mostrarError(traducirErrorPago(e))
    });
  }

  private interpretar(pago: Pago): Resultado {
    // El pedido manda: si ya está pagado (por este intento o por otro), se muestra como pagado.
    if (pago.estado === 'APROBADO' || !['PENDIENTE_PAGO', 'CANCELADO'].includes(pago.estadoPedido)) {
      return 'pagado';
    }
    switch (pago.estado) {
      case 'PENDIENTE':
        return 'procesando';
      case 'RECHAZADO':
        return 'rechazado';
      default:
        return 'sin-pago';
    }
  }

  private mostrarError(error: ErrorTraducido): void {
    this.error.set(error);
    this.resultado.set('error');
  }
}
