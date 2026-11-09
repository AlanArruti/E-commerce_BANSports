import { Rol } from '../app/models/rol';

/** Clave donde AuthService guarda el token (ver auth.service.ts). */
export const CLAVE_TOKEN = 'ban_token';

interface DatosToken {
  sub?: string;
  rol?: Rol;
  id?: number;
  /** Segundos desde ahora hasta que vence. Negativo = ya vencido. */
  venceEn?: number;
}

function base64Url(objeto: object): string {
  return btoa(JSON.stringify(objeto)).replace(/=+$/, '').replace(/\+/g, '-').replace(/\//g, '_');
}

/**
 * Arma un JWT con el formato que espera el front. La firma es falsa: el front no la verifica
 * (eso lo hace el back), así que alcanza para los tests.
 */
export function crearToken({ sub = 'ana@mail.com', rol = 'CLIENTE', id = 1, venceEn = 3600 }: DatosToken = {}): string {
  const ahora = Math.floor(Date.now() / 1000);
  return `${base64Url({ alg: 'HS256' })}.${base64Url({ sub, rol, id, iat: ahora, exp: ahora + venceEn })}.firma-de-prueba`;
}

/** Deja un token guardado como si el usuario hubiera iniciado sesión antes (AuthService lo lee al crearse). */
export function guardarToken(token: string): void {
  localStorage.setItem(CLAVE_TOKEN, token);
}

/** Borra cualquier sesión guardada. Se llama antes y después de cada test para que no se pisen. */
export function limpiarSesion(): void {
  localStorage.removeItem(CLAVE_TOKEN);
  sessionStorage.removeItem(CLAVE_TOKEN);
}
