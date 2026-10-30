import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from '../services/auth.service';

/**
 * Agrega el JWT a cada pedido a nuestra API.
 * Si el back responde 401 (token vencido o alterado), cierra la sesión y manda al login.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const esNuestraApi = req.url.startsWith(environment.apiUrl);
  const esAuth = req.url.startsWith(`${environment.apiUrl}/auth/`);
  // En el cambio de contraseña un 401 significa "contraseña actual incorrecta",
  // no que el token sea inválido: lo maneja el formulario del perfil.
  const esCambioPassword = /\/cliente\/\d+\/password$/.test(req.url);
  const token = authService.getToken();

  const pedido =
    esNuestraApi && !esAuth && token
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(pedido).pipe(
    catchError((error: unknown) => {
      // En /auth un 401 significa "credenciales incorrectas": lo maneja el formulario.
      const mensaje =
        error instanceof HttpErrorResponse && typeof error.error?.message === 'string'
          ? error.error.message.toLowerCase()
          : '';
      // Solo la contraseña actual incorrecta no invalida la sesión.
      const esPasswordIncorrecta = esCambioPassword && mensaje.includes('contraseña actual');

      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        esNuestraApi &&
        !esAuth &&
        !esPasswordIncorrecta
      ) {
        authService.logout(true);
        router.navigate(['/login'], { queryParams: { redirect: router.url } });
      }
      return throwError(() => error);
    })
  );
};
