import { Direccion } from './direccion';

export interface Cliente {
  id: number;
  uuidCliente: string;
  nombre: string;
  apellido: string;
  dni: string;
  email: string;
  direcciones: Direccion[];
  /** Fecha de registro (ISO, sin zona). A los clientes anteriores a la columna les quedó la fecha de la migración. */
  fechaAlta?: string;
}
