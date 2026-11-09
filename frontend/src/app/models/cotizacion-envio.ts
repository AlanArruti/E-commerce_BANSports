// Opción de envío devuelta por el back (CotizacionEnvioResponse).
export interface CotizacionEnvio {
  transportista: string;
  tipoEntrega: string;
  servicio: string;
  precio: number;
  plazoMinimoDias: number;
  plazoMaximoDias: number;
}

/** "3–5 días" o "2 días" si el mínimo y el máximo coinciden. */
export function plazoDeEnvio(opcion: CotizacionEnvio): string {
  const minimo = opcion.plazoMinimoDias ?? 0;
  const maximo = opcion.plazoMaximoDias ?? minimo;
  const dias = minimo === maximo ? `${minimo}` : `${minimo}–${maximo}`;
  return `${dias} ${maximo === 1 ? 'día' : 'días'}`;
}
