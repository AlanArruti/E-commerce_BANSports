import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '../services/auth.service';

/** Para /login y /registro: si ya hay una sesión válida, manda al home. */
export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.validarSesion().pipe(
    map((sesionValida) => (sesionValida ? router.createUrlTree(['/']) : true))
  );
};
