import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Exige una sesión confirmada por el back.
 * Si no hay sesión (o el token fue alterado), manda al login y recuerda a dónde quería ir.
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.validarSesion().pipe(
    map((sesionValida) =>
      sesionValida ? true : router.createUrlTree(['/login'], { queryParams: { redirect: state.url } })
    )
  );
};
