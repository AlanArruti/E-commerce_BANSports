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

export const ETIQUETAS_ESTADO_ENVIO: Record<EstadoEnvio, string> = {
  DESPACHADO: 'Despachado',
  EN_CAMINO: 'En camino',
  ENTREGADO: 'Entregado'
};

/** Orden en que avanza un envío. */
export const PASOS_ENVIO: EstadoEnvio[] = ['DESPACHADO', 'EN_CAMINO', 'ENTREGADO'];

/** Próximo estado del envío, o null si ya se entregó. */
export function siguienteEstadoEnvio(estado: EstadoEnvio): EstadoEnvio | null {
  const i = PASOS_ENVIO.indexOf(estado);
  return i >= 0 && i < PASOS_ENVIO.length - 1 ? PASOS_ENVIO[i + 1] : null;
}

/** Empresas que se sugieren al despachar. Se puede escribir otra. */
export const EMPRESAS_ENVIO = ['Correo Argentino', 'Andreani', 'OCA'];

// Envío del pedido (EnvioResponse). Lo crea el admin al despachar; antes es null.
export interface Envio {
  id: number;
  idPedido: number;
  /** "Juan Pérez". */
  cliente: string | null;
  direccion: Direccion | null;
  estado: EstadoEnvio;
  fechaSalida: string | null;
  fechaLlegada: string | null;
  /** Null en los envíos de antes de SCRUM-139. */
  empresa: string | null;
  numeroSeguimiento: string | null;
}

export type TipoFactura = 'A' | 'B' | 'C';

// Factura del pedido (FacturaResponse). Se emite al cobrar; antes es null.
export interface Factura {
  id: number;
  uuidFactura: string;
  tipo: TipoFactura;
  precioTotal: number;
  idPedido: number;
  /** Null en las facturas de antes de SCRUM-140. */
  fechaEmision: string | null;
}

/** El back no numera las facturas: se arma con el punto de venta 0001 y el id. "0001-00000004" */
export function numeroFactura(factura: Factura): string {
  return `0001-${String(factura.id).padStart(8, '0')}`;
}

/** El cliente cancela solo lo que todavía no pagó (lo cobrado lo cancela la tienda). */
export function clientePuedeCancelar(estado: EstadoPedido): boolean {
  return estado === 'PENDIENTE_PAGO' || estado === 'EN_PREPARACION';
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
  empresa: string;
  numeroSeguimiento: string;
  fechaSalida: string;
  fechaLlegada: string;
}

/** Si el pedido ya se había cobrado, el back devuelve el stock al cancelarlo. */
export function cancelarDevuelveStock(estado: EstadoPedido): boolean {
  return estado === 'PAGADO' || estado === 'DESPACHADO';
}
