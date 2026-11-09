import { EstadoPedido } from './pedido';

/** Períodos que acepta GET /admin/metricas?dias=... */
export type DiasMetricas = 7 | 30 | 90;

// Un día del período; los días sin ventas vienen en cero. `fecha` es "AAAA-MM-DD".
export interface VentaDiaria {
  fecha: string;
  total: number;
  pedidos: number;
}

export interface PedidosPorEstado {
  estado: EstadoPedido;
  cantidad: number;
}

// `total` es lo facturado por el producto, sin el envío.
export interface ProductoMasVendido {
  idProducto: number;
  nombre: string;
  unidades: number;
  total: number;
}

// Respuesta de GET /admin/metricas (MetricasAdminResponse).
export interface MetricasAdmin {
  dias: DiasMetricas;
  ventasPeriodo: number;
  ventasPeriodoAnterior: number;
  pedidosPeriodo: number;
  pedidosPeriodoAnterior: number;
  ventasPorDia: VentaDiaria[];
  pedidosPorEstado: PedidosPorEstado[];
  masVendidos: ProductoMasVendido[];
}
