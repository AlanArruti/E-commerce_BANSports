import { Cliente } from './cliente';
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

export type TipoFactura = 'A' | 'B' | 'C';

// Factura del pedido (FacturaResponse). Se emite al cobrar; antes es null.
export interface Factura {
  id: number;
  uuidFactura: string;
  tipo: TipoFactura;
  precioTotal: number;
  idPedido: number;
}

// Respuesta de POST /cliente/{id}/carrito/checkout, GET /pedido/{id} y del historial (PedidoResponse).
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
  /** Lo usa el panel del admin. */
  cliente: Cliente | null;
  factura: Factura | null;
}

/** Cambios de estado que el admin hace a mano (PATCH /pedido/{id}/estado). PAGADO nunca: se llega cobrando. */
export type EstadoManual = Exclude<EstadoPedido, 'PAGADO'>;

// Body de POST /pedido/{id}/envio (EnvioRequest). Fechas en formato AAAA-MM-DD.
export interface EnvioRequest {
  fechaSalida: string;
  fechaLlegada: string;
}

/** Si el pedido ya se había cobrado, el back devuelve el stock al cancelarlo. */
export function cancelarDevuelveStock(estado: EstadoPedido): boolean {
  return estado === 'PAGADO' || estado === 'DESPACHADO';
}
