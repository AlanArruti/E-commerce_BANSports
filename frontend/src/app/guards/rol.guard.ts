import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { Rol } from '../models/rol';
import { AuthService } from '../services/auth.service';

/**
 * Permite entrar solo a usuarios con el rol indicado.
 * El rol se lee del token, y el token se confirma con el back antes de decidir.
 * Uso: { path: 'panel', component: Panel, canActivate: [rolGuard('ADMIN')] }
 */
export function rolGuard(rolPermitido: Rol): CanActivateFn {
  return (_route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    return authService.validarSesion().pipe(
      map((sesionValida) => {
        if (!sesionValida) {
          return router.createUrlTree(['/login'], { queryParams: { redirect: state.url } });
        }
        return authService.usuario()?.rol === rolPermitido ? true : router.createUrlTree(['/']);
      })
    );
  };
}
