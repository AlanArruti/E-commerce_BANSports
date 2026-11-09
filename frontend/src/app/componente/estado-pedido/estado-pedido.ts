import { Component, computed, input, ChangeDetectionStrategy } from '@angular/core';
import { ETIQUETAS_ESTADO_PEDIDO, EstadoPedido } from '../../models/pedido';

/** Etiqueta de color con el estado del pedido ("Pendiente de pago", "Entregado"…). */
@Component({
  selector: 'app-estado-pedido',
  template: `<span class="estado" [class]="'estado estado-' + clase()">{{ etiqueta() }}</span>`,
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './estado-pedido.css'
})
export class EstadoPedidoEtiqueta {
  readonly estado = input.required<EstadoPedido>();

  protected readonly etiqueta = computed(() => ETIQUETAS_ESTADO_PEDIDO[this.estado()] ?? this.estado());
  protected readonly clase = computed(() => this.estado().toLowerCase().replace('_', '-'));
}
