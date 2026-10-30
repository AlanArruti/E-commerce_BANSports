import { Direccion } from './direccion';

export interface Cliente {
  id: number;
  uuidCliente: string;
  nombre: string;
  apellido: string;
  dni: string;
  email: string;
  direcciones: Direccion[];
}
