import { Rol } from './rol';

// Respuesta de GET /auth/me: quién es el dueño del token.
export interface UsuarioActual {
  id: number;
  username: string; // email del cliente o username del admin
  rol: Rol;
}
