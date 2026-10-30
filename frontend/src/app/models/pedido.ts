export type EstadoPedido =
  | 'PENDIENTE_PAGO'
  | 'PAGADO'
  | 'EN_PREPARACION'
  | 'DESPACHADO'
  | 'ENTREGADO'
  | 'CANCELADO';

export interface ItemPedidoResumen {
  cantidad: number;
}

export interface Pedido {
  id: number;
  uuidPedido: string;
  fechaCreacion: string;
  total: number;
  estado: EstadoPedido;
  items: ItemPedidoResumen[];
}
