/** Cotización del dólar (GET /api/v1/dolar). RF01. */
export interface CotizacionDolar {
  /** Tipo de dólar: oficial, blue, bolsa... */
  casa: string;
  compra: number | null;
  venta: number;
  /** Cuándo actualizó el valor la fuente (DolarApi). */
  fechaActualizacion: string | null;
  /** Última vez que el back la consultó. */
  fechaConsulta: string;
  /** Dólar con el que se ajustaron los precios por última vez. */
  cotizacionReferencia: number | null;
  fechaUltimoAjuste: string | null;
  /** Cuánto se movió el dólar desde ese ajuste, en % (positivo = subió). */
  variacionPorcentaje: number | null;
  /** Desde qué variación se ajustan los precios solos. */
  ajusteMinimoPorcentaje: number;
  /** Desde qué variación el ajuste lo tiene que confirmar el admin. */
  variacionMaximaPorcentaje: number;
}

export type ResultadoAjuste = 'BASE' | 'AJUSTADO' | 'SIN_CAMBIOS' | 'REQUIERE_CONFIRMACION';

/** Respuesta de POST /api/v1/dolar/actualizar. */
export interface ActualizacionDolar {
  cotizacion: CotizacionDolar;
  resultado: ResultadoAjuste;
  porcentajeAjuste: number;
  productosAjustados: number;
  mensaje: string;
}

/** Un ajuste de precios del historial (GET /api/v1/dolar/historial). */
export interface AjustePrecios {
  casa: string;
  venta: number;
  porcentajeAjuste: number;
  productosAjustados: number;
  fechaAjuste: string;
}

/** Nombre para mostrar de cada tipo de dólar. */
export const NOMBRES_DOLAR: Record<string, string> = {
  oficial: 'Dólar oficial',
  blue: 'Dólar blue',
  bolsa: 'Dólar MEP',
  contadoconliqui: 'Dólar CCL',
  mayorista: 'Dólar mayorista',
  cripto: 'Dólar cripto',
  tarjeta: 'Dólar tarjeta'
};

export function nombreDolar(casa: string): string {
  return NOMBRES_DOLAR[casa] ?? `Dólar ${casa}`;
}

const FORMATO_PORCENTAJE = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 1 });

/** 0.5 → "▲ 0,5%", -1.2 → "▼ 1,2%", 0 → "0%". */
export function formatearVariacion(porcentaje: number): string {
  const redondeado = Math.round(porcentaje * 10) / 10;
  if (redondeado === 0) {
    return '0%';
  }
  return `${redondeado > 0 ? '▲' : '▼'} ${FORMATO_PORCENTAJE.format(Math.abs(redondeado))}%`;
}

export function tendencia(porcentaje: number | null): 'sube' | 'baja' | 'igual' {
  const redondeado = Math.round((porcentaje ?? 0) * 10) / 10;
  return redondeado > 0 ? 'sube' : redondeado < 0 ? 'baja' : 'igual';
}

// hourCycle explícito: según el navegador, es-AR puede salir en 12 h ("03:05 p. m.").
const HORA = new Intl.DateTimeFormat('es-AR', { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' });
const DIA = new Intl.DateTimeFormat('es-AR', { day: '2-digit', month: '2-digit' });

/** "hoy 15:00", "ayer 09:30" o "20/11 10:30". */
export function formatearFecha(iso: string | null, ahora: Date = new Date()): string {
  if (!iso) {
    return '';
  }
  const fecha = new Date(iso);
  const dias = Math.round((inicioDelDia(ahora) - inicioDelDia(fecha)) / 86_400_000);
  const hora = HORA.format(fecha);
  if (dias === 0) {
    return `hoy ${hora}`;
  }
  if (dias === 1) {
    return `ayer ${hora}`;
  }
  return `${DIA.format(fecha)} ${hora}`;
}

function inicioDelDia(fecha: Date): number {
  return new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate()).getTime();
}
