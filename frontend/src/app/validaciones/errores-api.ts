import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models/api-error';

// Traduce los errores HTTP del back a mensajes claros para el usuario.

export interface ErrorTraducido {
  titulo: string;
  detalle: string;
  /** Si el error corresponde a un campo puntual del formulario. */
  campo?: 'email' | 'dni';
}

export const MSJ_SOLO_CLIENTES: ErrorTraducido = {
  titulo: 'Este acceso es solo para clientes',
  detalle: 'Los administradores ingresan desde su propio panel.'
};

export const MSJ_SESION_INVALIDA = 'Tu sesión expiró o no es válida. Iniciá sesión de nuevo.';

function mensajeDelBack(error: HttpErrorResponse): string {
  const cuerpo = error.error as Partial<ApiError> | null;
  return typeof cuerpo?.message === 'string' ? cuerpo.message : '';
}

/** Errores comunes a cualquier pedido (sin conexión, servidor caído). */
function errorGeneral(error: HttpErrorResponse): ErrorTraducido | null {
  if (error.status === 0) {
    return {
      titulo: 'No pudimos conectarnos con el servidor',
      detalle: 'Revisá tu conexión a internet e intentá de nuevo.'
    };
  }
  if (error.status >= 500) {
    return {
      titulo: 'Ocurrió un error en el servidor',
      detalle: 'No es un problema de tus datos. Intentá de nuevo en unos minutos.'
    };
  }
  return null;
}

export function traducirErrorLogin(error: HttpErrorResponse): ErrorTraducido {
  const general = errorGeneral(error);
  if (general) {
    return general;
  }
  const mensaje = mensajeDelBack(error);

  if (error.status === 429) {
    return {
      titulo: 'Ingreso bloqueado temporalmente',
      detalle: mensaje || 'Hubo demasiados intentos fallidos. Esperá 15 minutos e intentá de nuevo.'
    };
  }
  if (error.status === 401 && mensaje.toLowerCase().includes('desactivada')) {
    return {
      titulo: 'Tu cuenta está desactivada',
      detalle: 'Escribinos para reactivarla.'
    };
  }
  if (error.status === 401 || error.status === 404) {
    return {
      titulo: 'El email o la contraseña son incorrectos',
      detalle: 'Revisalos y probá de nuevo. Después de 5 intentos fallidos se bloquea el ingreso por 15 minutos.'
    };
  }
  return {
    titulo: 'No pudimos iniciar sesión',
    detalle: mensaje || `El servidor respondió con el código ${error.status}.`
  };
}

export function traducirErrorRegistro(error: HttpErrorResponse): ErrorTraducido {
  const general = errorGeneral(error);
  if (general) {
    return general;
  }
  const mensaje = mensajeDelBack(error);

  if (error.status === 409) {
    if (mensaje.toLowerCase().includes('email')) {
      return {
        titulo: 'Ese email ya tiene una cuenta',
        detalle: 'Ya existe una cuenta con este email.',
        campo: 'email'
      };
    }
    if (mensaje.toLowerCase().includes('dni')) {
      return {
        titulo: 'Ese DNI ya tiene una cuenta',
        detalle: 'Ya existe una cuenta con este DNI.',
        campo: 'dni'
      };
    }
  }
  if (error.status === 400 && mensaje) {
    return { titulo: 'Revisá los datos ingresados', detalle: mensaje };
  }
  return {
    titulo: 'No pudimos crear tu cuenta',
    detalle: mensaje || `El servidor respondió con el código ${error.status}.`
  };
}
