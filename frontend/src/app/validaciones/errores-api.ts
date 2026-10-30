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

export const MSJ_SOLO_ADMINS: ErrorTraducido = {
  titulo: 'Este acceso es solo para administradores',
  detalle: 'Si sos cliente, ingresá desde la tienda.'
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

/** @param credencial cómo se llama el dato de ingreso en ese login (email para clientes, usuario para admins). */
export function traducirErrorLogin(
  error: HttpErrorResponse,
  credencial: 'email' | 'usuario' = 'email'
): ErrorTraducido {
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
      titulo: `El ${credencial} o la contraseña son incorrectos`,
      detalle: 'Revisalos y probá de nuevo. Después de 5 intentos fallidos se bloquea el ingreso por 15 minutos.'
    };
  }
  return {
    titulo: 'No pudimos iniciar sesión',
    detalle: mensaje || `El servidor respondió con el código ${error.status}.`
  };
}

export function traducirErrorPerfil(error: HttpErrorResponse): ErrorTraducido {
  const general = errorGeneral(error);
  if (general) {
    return general;
  }
  const mensaje = mensajeDelBack(error);

  if (error.status === 409) {
    return {
      titulo: 'No pudimos guardar los cambios',
      detalle: 'Ese email ya está registrado por otra cuenta.'
    };
  }
  if (error.status === 403) {
    return {
      titulo: 'No tenés permiso para editar este perfil',
      detalle: 'Iniciá sesión de nuevo con tu cuenta.'
    };
  }
  if (error.status === 400 && mensaje) {
    return {
      titulo: 'Revisá los datos ingresados',
      detalle: mensaje
    };
  }
  return {
    titulo: 'No pudimos actualizar tus datos',
    detalle: mensaje || `El servidor respondió con el código ${error.status}.`
  };
}

export function traducirErrorPassword(error: HttpErrorResponse): ErrorTraducido {
  const general = errorGeneral(error);
  if (general) {
    return general;
  }
  const mensaje = mensajeDelBack(error);

  if (error.status === 401) {
    return {
      titulo: 'La contraseña actual no es correcta',
      detalle: 'Revisala y volvé a intentar.'
    };
  }
  if (error.status === 400 && mensaje) {
    return {
      titulo: 'Revisá la contraseña nueva',
      detalle: mensaje
    };
  }
  return {
    titulo: 'No pudimos cambiar la contraseña',
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

/** El 404 no pasa por acá: la página de detalle muestra "Producto no encontrado". */
export function traducirErrorProducto(error: HttpErrorResponse): ErrorTraducido {
  return (
    errorGeneral(error) ?? {
      titulo: 'No pudimos cargar el producto',
      detalle: mensajeDelBack(error) || `El servidor respondió con el código ${error.status}.`
    }
  );
}

export function traducirErrorCarrito(error: HttpErrorResponse): ErrorTraducido {
  const general = errorGeneral(error);
  if (general) {
    return general;
  }
  const mensaje = mensajeDelBack(error);

  // El back responde "Stock insuficiente para la variante id 3. Disponible: 25, solicitado: 26."
  const disponible = /Disponible:\s*(\d+)/.exec(mensaje)?.[1];
  if (error.status === 400 && disponible !== undefined) {
    return {
      titulo: 'No hay stock suficiente',
      detalle: `Quedan ${disponible} unidades de esta variante, contando las que ya tenés en el carrito.`
    };
  }
  if (error.status === 404) {
    return {
      titulo: 'Ese producto ya no está disponible',
      detalle: 'Puede que lo hayan dado de baja. Actualizá la página para ver el carrito al día.'
    };
  }
  return {
    titulo: 'No pudimos actualizar el carrito',
    detalle: mensaje || `El servidor respondió con el código ${error.status}.`
  };
}

export function traducirErrorCheckout(error: HttpErrorResponse): ErrorTraducido {
  const general = errorGeneral(error);
  if (general) {
    return general;
  }
  const mensaje = mensajeDelBack(error);

  const disponible = /Disponible:\s*(\d+)/.exec(mensaje)?.[1];
  if (error.status === 400 && disponible !== undefined) {
    return {
      titulo: 'No hay stock suficiente',
      detalle: `De uno de los productos quedan ${disponible} unidades. Ajustá el carrito y volvé a confirmar.`
    };
  }
  if (error.status === 409) {
    return {
      titulo: 'Tu carrito está vacío',
      detalle: 'Agregá productos antes de confirmar el pedido.'
    };
  }
  if (error.status === 403 || error.status === 404) {
    return {
      titulo: 'No pudimos usar esa dirección',
      detalle: 'Puede que la hayan borrado. Actualizá la página y elegí otra.'
    };
  }
  return {
    titulo: 'No pudimos confirmar el pedido',
    detalle: mensaje || `El servidor respondió con el código ${error.status}.`
  };
}

export function traducirErrorDireccion(error: HttpErrorResponse): ErrorTraducido {
  const general = errorGeneral(error);
  if (general) {
    return general;
  }
  const mensaje = mensajeDelBack(error);
  if (error.status === 400 && mensaje) {
    return { titulo: 'Revisá los datos de la dirección', detalle: mensaje };
  }
  return {
    titulo: 'No pudimos guardar la dirección',
    detalle: mensaje || `El servidor respondió con el código ${error.status}.`
  };
}

/** El 409 (nombre repetido) lo muestra el formulario junto al campo; acá llega el resto. */
export function traducirErrorCategoria(error: HttpErrorResponse): ErrorTraducido {
  const general = errorGeneral(error);
  if (general) {
    return general;
  }
  const mensaje = mensajeDelBack(error);
  if (error.status === 404) {
    return {
      titulo: 'Esa categoría ya no existe',
      detalle: 'Puede que la haya borrado otro administrador. La lista se actualizó.'
    };
  }
  if (error.status === 400 && mensaje) {
    return { titulo: 'Revisá el nombre de la categoría', detalle: mensaje };
  }
  return {
    titulo: 'No pudimos guardar los cambios',
    detalle: mensaje || `El servidor respondió con el código ${error.status}.`
  };
}

export function traducirErrorGuardarProducto(error: HttpErrorResponse): ErrorTraducido {
  const general = errorGeneral(error);
  if (general) {
    return general;
  }
  const mensaje = mensajeDelBack(error);
  if (error.status === 404) {
    return {
      titulo: 'No encontramos el producto o la categoría',
      detalle: 'Puede que los hayan dado de baja mientras editabas. Volvé al panel y probá de nuevo.'
    };
  }
  if (error.status === 400 && mensaje) {
    return { titulo: 'Revisá los datos del producto', detalle: mensaje };
  }
  return {
    titulo: 'No pudimos guardar el producto',
    detalle: mensaje || `El servidor respondió con el código ${error.status}.`
  };
}

export function traducirErrorBajaProducto(error: HttpErrorResponse): ErrorTraducido {
  const general = errorGeneral(error);
  if (general) {
    return general;
  }
  if (error.status === 404) {
    return {
      titulo: 'Ese producto ya estaba dado de baja',
      detalle: 'Puede que lo haya dado de baja otro administrador. La lista se actualizó.'
    };
  }
  return {
    titulo: 'No pudimos dar de baja el producto',
    detalle: mensajeDelBack(error) || `El servidor respondió con el código ${error.status}.`
  };
}

export function traducirErrorCatalogo(error: HttpErrorResponse): ErrorTraducido {
  return (
    errorGeneral(error) ?? {
      titulo: 'No pudimos cargar los productos',
      detalle: mensajeDelBack(error) || `El servidor respondió con el código ${error.status}.`
    }
  );
}
