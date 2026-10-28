import { Rol } from './rol';

// Datos que el back guarda dentro del JWT (ver JwtUtil.generarToken).
export interface JwtPayload {
  sub: string; // email del cliente o username del admin
  rol: Rol;
  id: number;
  iat: number; // emitido (segundos desde 1970)
  exp: number; // vence (segundos desde 1970)
}
