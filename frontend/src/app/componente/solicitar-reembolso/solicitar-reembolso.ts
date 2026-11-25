import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Pedido, puedeSolicitarReembolso } from '../../models/pedido';
import { PrecioPipe } from '../../pipes/precio.pipe';
import { PedidoService } from '../../services/pedido.service';
import { ErrorTraducido, traducirErrorReembolso } from '../../validaciones/errores-api';

/** Largos del motivo (iguales a SolicitudReembolsoRequest en el back). */
export const MINIMO_MOTIVO = 10;
const MAXIMO_MOTIVO = 500;

/**
 * RF05: en un pedido entregado, el cliente pide el reembolso con un motivo (hasta 10 días
 * después de la entrega) y después ve cómo lo resolvió la tienda. Lo usan Mis pedidos y el
 * detalle del pedido. Si no hay nada para mostrar (no entregado, o plazo vencido sin pedirlo), no muestra nada.
 */
@Component({
  selector: 'app-solicitar-reembolso',
  imports: [DatePipe, PrecioPipe],
  templateUrl: './solicitar-reembolso.html',
  styleUrl: './solicitar-reembolso.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SolicitarReembolso {
  private readonly pedidoService = inject(PedidoService);

  readonly pedido = input.required<Pedido>();
  /** El pedido como quedó después de pedir el reembolso. */
  readonly solicitado = output<Pedido>();

  protected readonly minimo = MINIMO_MOTIVO;
  protected readonly maximo = MAXIMO_MOTIVO;
  protected readonly escribiendo = signal(false);
  protected readonly motivo = signal('');
  protected readonly intento = signal(false);
  protected readonly enviando = signal(false);
  protected readonly error = signal<ErrorTraducido | null>(null);

  protected readonly puedePedir = computed(() => puedeSolicitarReembolso(this.pedido()));
  protected readonly motivoCorto = computed(() => this.motivo().trim().length < MINIMO_MOTIVO);

  protected abrir(): void {
    this.error.set(null);
    this.intento.set(false);
    this.escribiendo.set(true);
  }

  protected volver(): void {
    this.escribiendo.set(false);
    this.motivo.set('');
  }

  protected escribir(evento: Event): void {
    this.motivo.set((evento.target as HTMLTextAreaElement).value);
  }

  protected enviar(): void {
    this.intento.set(true);
    if (this.motivoCorto()) {
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    this.pedidoService.solicitarReembolso(this.pedido().id, this.motivo().trim()).subscribe({
      next: (pedido) => {
        this.enviando.set(false);
        this.escribiendo.set(false);
        this.motivo.set('');
        this.solicitado.emit(pedido);
      },
      error: (e: unknown) => {
        this.enviando.set(false);
        this.error.set(
          e instanceof HttpErrorResponse
            ? traducirErrorReembolso(e)
            : { titulo: 'No pudimos enviar el pedido de reembolso', detalle: 'Ocurrió un error inesperado. Intentá de nuevo.' }
        );
      }
    });
  }
}
