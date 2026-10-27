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
  const token = authService.getToken();

  const pedido =
    esNuestraApi && !esAuth && token
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(pedido).pipe(
    catchError((error: unknown) => {
      // En /auth un 401 significa "credenciales incorrectas": lo maneja el formulario.
      if (error instanceof HttpErrorResponse && error.status === 401 && esNuestraApi && !esAuth) {
        authService.logout(true);
        router.navigate(['/login'], { queryParams: { redirect: router.url } });
      }
      return throwError(() => error);
    })
  );
};
