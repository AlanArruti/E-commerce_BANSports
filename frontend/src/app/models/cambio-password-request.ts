export interface CambioPasswordRequest {
  /** Null si la cuenta se creó con Google o GitHub y está creando su primera contraseña. */
  contraseniaActual: string | null;
  contraseniaNueva: string;
}
