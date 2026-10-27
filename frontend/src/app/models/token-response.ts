import { Rol } from './rol';

// Respuesta de POST /auth/login.
export interface TokenResponse {
  token: string;
  tipo: string;
  rol: Rol;
  id: number;
}
