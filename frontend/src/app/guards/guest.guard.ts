import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { INICIO_POR_ROL } from './rol.guard';

/** Para las pantallas de login y registro: si ya hay una sesión válida, manda al inicio de su rol. */
export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.validarSesion().pipe(
    map((sesionValida) => {
      const rol = authService.usuario()?.rol;
      return sesionValida && rol ? router.createUrlTree([INICIO_POR_ROL[rol]]) : true;
    })
  );
};
