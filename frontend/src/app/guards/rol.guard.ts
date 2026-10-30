import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { Rol } from '../models/rol';
import { AuthService } from '../services/auth.service';

/** Pantalla de inicio de cada rol: a dónde se manda a alguien que entra donde no le toca. */
export const INICIO_POR_ROL: Record<Rol, string> = {
  CLIENTE: '/',
  ADMIN: '/admin'
};

/** Login que corresponde a cada rol. */
const LOGIN_POR_ROL: Record<Rol, string> = {
  CLIENTE: '/login',
  ADMIN: '/admin/login'
};

/**
 * Permite entrar solo a usuarios con el rol indicado.
 * El rol se lee del token, y el token se confirma con el back antes de decidir.
 * - Sin sesión: va al login de ese rol y, al ingresar, vuelve a donde quería ir.
 * - Con otro rol: va a su propia pantalla de inicio (un cliente en /admin vuelve a la tienda).
 * Uso: { path: 'admin', canActivate: [rolGuard('ADMIN')], children: [...] }
 */
export function rolGuard(rolPermitido: Rol): CanActivateFn {
  return (_route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    return authService.validarSesion().pipe(
      map((sesionValida) => {
        if (!sesionValida) {
          return router.createUrlTree([LOGIN_POR_ROL[rolPermitido]], { queryParams: { redirect: state.url } });
        }
        const rol = authService.usuario()?.rol;
        if (rol === rolPermitido) {
          return true;
        }
        return router.createUrlTree([rol ? INICIO_POR_ROL[rol] : '/']);
      })
    );
  };
}
