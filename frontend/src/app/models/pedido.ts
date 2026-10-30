import { Producto } from './producto';
import { Variante } from './variante';

export type EstadoPedido = 'PENDIENTE_PAGO' | 'PAGADO' | 'EN_PREPARACION' | 'DESPACHADO' | 'ENTREGADO' | 'CANCELADO';

/** Cómo se muestra cada estado al cliente. */
export const ETIQUETAS_ESTADO_PEDIDO: Record<EstadoPedido, string> = {
  PENDIENTE_PAGO: 'Pendiente de pago',
  PAGADO: 'Pagado',
  EN_PREPARACION: 'En preparación',
  DESPACHADO: 'Despachado',
  ENTREGADO: 'Entregado',
  CANCELADO: 'Cancelado'
};

// Ítem del pedido (ItemPedidoResponse). El precio queda fijo al momento de la compra.
export interface ItemPedido {
  idItemPedido: number;
  producto: Producto;
  variante: Variante | null;
  cantidad: number;
  precioTotalProducto: number;
}

// Respuesta de POST /cliente/{id}/carrito/checkout y GET /pedido/{id} (PedidoResponse).
// Solo los campos que usa el front; el back también manda el cliente, el envío y la factura.
export interface Pedido {
  id: number;
  uuidPedido: string;
  fechaCreacion: string;
  items: ItemPedido[];
  total: number;
  estado: EstadoPedido;
}
