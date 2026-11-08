import { Direccion } from './direccion';
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

/** Orden en que avanza un pedido. CANCELADO queda afuera: corta el recorrido. */
export const PASOS_PEDIDO: EstadoPedido[] = ['PENDIENTE_PAGO', 'PAGADO', 'EN_PREPARACION', 'DESPACHADO', 'ENTREGADO'];

export type EstadoEnvio = 'DESPACHADO' | 'EN_CAMINO' | 'ENTREGADO';

// Envío del pedido (EnvioResponse). Lo crea el admin al despachar; antes es null.
export interface Envio {
  id: number;
  direccion: Direccion | null;
  estado: EstadoEnvio;
  fechaSalida: string | null;
  fechaLlegada: string | null;
}

// Respuesta de POST /cliente/{id}/carrito/checkout, GET /pedido/{id} y del historial (PedidoResponse).
// Solo los campos que usa el front; el back también manda el cliente y la factura.
export interface Pedido {
  id: number;
  uuidPedido: string;
  fechaCreacion: string;
  items: ItemPedido[];
  total: number;
  estado: EstadoPedido;
  /** Dirección elegida en el checkout. */
  direccionEnvio: Direccion | null;
  envio: Envio | null;
}
