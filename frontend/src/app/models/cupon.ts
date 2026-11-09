export type TipoCupon = 'PORCENTAJE' | 'MONTO_FIJO';

export type EstadoCupon = 'VIGENTE' | 'PROGRAMADO' | 'VENCIDO' | 'AGOTADO' | 'INACTIVO';

export const ETIQUETAS_ESTADO_CUPON: Record<EstadoCupon, string> = {
  VIGENTE: 'Vigente',
  PROGRAMADO: 'Programado',
  VENCIDO: 'Vencido',
  AGOTADO: 'Agotado',
  INACTIVO: 'Dado de baja'
};

// Respuesta de GET /cupones (CuponResponse). Las fechas vienen como "AAAA-MM-DD".
export interface Cupon {
  id: number;
  codigo: string;
  tipo: TipoCupon;
  /** Porcentaje (1 a 100) o monto en pesos, según el tipo. */
  valor: number;
  vigenteDesde: string | null;
  vigenteHasta: string | null;
  /** Subtotal de productos mínimo. 0 = sin mínimo. */
  montoMinimo: number;
  /** null = sin límite. */
  usosMaximos: number | null;
  usos: number;
  activo: boolean;
  estado: EstadoCupon;
}

// Cuerpo de POST y PUT /cupones (CuponRequest).
export interface CuponRequest {
  codigo: string;
  tipo: TipoCupon;
  valor: number;
  vigenteDesde: string | null;
  vigenteHasta: string | null;
  montoMinimo: number | null;
  usosMaximos: number | null;
  activo: boolean;
}

// Respuesta de GET /cupones/validar: el cupón sirve para el carrito actual.
export interface ValidacionCupon {
  codigo: string;
  tipo: TipoCupon;
  valor: number;
  /** Subtotal de productos del carrito (sin envío). */
  subtotal: number;
  descuento: number;
}
