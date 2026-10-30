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
  // Login y registro van sin token. /auth/me sí lo necesita (con él se valida la sesión del admin).
  const esAuth = ['/auth/login', '/auth/register'].some((ruta) =>
    req.url.startsWith(`${environment.apiUrl}${ruta}`)
  );
  const token = authService.getToken();

  const pedido =
    esNuestraApi && !esAuth && token
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(pedido).pipe(
    catchError((error: unknown) => {
      // En /auth un 401 significa "credenciales incorrectas": lo maneja el formulario.
      if (error instanceof HttpErrorResponse && error.status === 401 && esNuestraApi && !esAuth) {
        // Se mira el rol antes de cerrar la sesión, para mandarlo al login que le corresponde.
        const login = authService.esAdmin() ? '/admin/login' : '/login';
        authService.logout(true);
        router.navigate([login], { queryParams: { redirect: router.url } });
      }
      return throwError(() => error);
    })
  );
};
