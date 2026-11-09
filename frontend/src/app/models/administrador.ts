// Respuesta de GET /administrador (AdministradorResponse).
export interface Administrador {
  idAdministrador: number;
  username: string;
}

// Cuerpo de POST /administrador (AdministradorRequest).
export interface AdministradorRequest {
  username: string;
  contrasenia: string;
}

/** Mismos límites que AdministradorRequest en el back. */
export const ADMIN_USUARIO_MAX = 50;
export const ADMIN_CONTRASENIA_MIN = 6;
export const ADMIN_CONTRASENIA_MAX = 50;
