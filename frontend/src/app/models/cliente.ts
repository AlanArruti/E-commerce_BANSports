import { Direccion } from './direccion';

export interface Cliente {
  id: number;
  uuidCliente: string;
  nombre: string;
  apellido: string;
  /** Null en las cuentas creadas con Google o GitHub hasta que el cliente lo carga en su perfil. */
  dni: string | null;
  email: string;
  direcciones: Direccion[];
  /** Fecha de registro (ISO, sin zona). A los clientes anteriores a la columna les quedó la fecha de la migración. */
  fechaAlta?: string;
  /** false: la cuenta se creó con Google o GitHub y todavía no tiene contraseña propia. */
  tieneContrasenia?: boolean;
}

/** El login con GitHub deja "-" de apellido cuando el nombre no trae apellido. */
export const APELLIDO_PENDIENTE = '-';

/** Le falta el DNI (obligatorio para comprar) o el apellido: pasa con las cuentas creadas con Google o GitHub. */
export function faltanDatos(cliente: Cliente | null): boolean {
  return !!cliente && (!cliente.dni || !cliente.apellido || cliente.apellido === APELLIDO_PENDIENTE);
}
