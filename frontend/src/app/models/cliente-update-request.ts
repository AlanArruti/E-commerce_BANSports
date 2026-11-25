export interface ClienteUpdateRequest {
  nombre: string;
  apellido: string;
  email: string;
  /** Solo si el cliente todavía no tiene DNI (cuentas creadas con Google o GitHub). Una vez cargado no se cambia. */
  dni?: string | null;
}
