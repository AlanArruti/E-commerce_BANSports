import { HttpErrorResponse, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { ConexionService } from '../services/conexion.service';

/**
 * Avisa a ConexionService si el back no responde (error de red o 5xx) y cuando vuelve a responder.
 * No toca el error: cada pantalla lo sigue recibiendo para decidir qué mostrar.
 */
export const conexionInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(environment.apiUrl)) {
    return next(req);
  }
  const conexion = inject(ConexionService);

  return next(req).pipe(
    tap({
      next: (evento) => {
        if (evento instanceof HttpResponse) {
          conexion.reportarExito();
        }
      },
      error: (error: unknown) => {
        if (!(error instanceof HttpErrorResponse)) {
          return;
        }
        if (error.status === 0) {
          conexion.reportarFallo('red');
        } else if (error.status >= 500) {
          conexion.reportarFallo('servidor');
        } else {
          // Un 4xx también es una respuesta: el back está andando.
          conexion.reportarExito();
        }
      }
    })
  );
};
