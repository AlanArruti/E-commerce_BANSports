import { Ciudad } from './geo';

// Dirección de entrega del cliente (DireccionResponse).
export interface Direccion {
  id: number;
  uuidDireccion: string;
  nombreCalle: string;
  numeroCalle: number;
  codigoPostal: string;
  ciudad: Ciudad;
}

/** "Mar del Plata, Buenos Aires · CP 7600" */
export function localidadDeDireccion(direccion: Direccion): string {
  return `${direccion.ciudad.nombreCiudad}, ${direccion.ciudad.provincia.nombreProvincia} · CP ${direccion.codigoPostal}`;
}
