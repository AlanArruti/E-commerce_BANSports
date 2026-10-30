import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models/api-error';
import { ErrorTraducido } from './errores-api';

// Reglas y mensajes de la imagen de producto. Tienen que coincidir con
// ProductoService.subirImagen del back (JPEG, PNG o WEBP de hasta 2 MB).

export const TIPOS_IMAGEN = ['image/jpeg', 'image/png', 'image/webp'];
export const EXTENSIONES_IMAGEN = '.jpg,.jpeg,.png,.webp';
export const TAMANIO_MAXIMO_IMAGEN = 2 * 1024 * 1024;

/** "1,4 MB" o "850 KB", para mostrar tamaños de archivo. */
export function formatearTamanio(bytes: number): string {
  if (bytes >= 1024 * 1024) {
    return `${(bytes / (1024 * 1024)).toLocaleString('es-AR', { maximumFractionDigits: 1 })} MB`;
  }
  return `${Math.max(1, Math.round(bytes / 1024))} KB`;
}

/** Valida el archivo antes de enviarlo. Devuelve el mensaje de error o null si está bien. */
export function validarImagen(archivo: File): string | null {
  if (!TIPOS_IMAGEN.includes(archivo.type)) {
    const extension = archivo.name.includes('.') ? archivo.name.split('.').pop()!.toLowerCase() : '';
    const elegido = extension ? ` Elegiste un archivo .${extension}.` : '';
    return `La imagen tiene que ser JPG, PNG o WEBP.${elegido}`;
  }
  if (archivo.size > TAMANIO_MAXIMO_IMAGEN) {
    return `La imagen pesa ${formatearTamanio(archivo.size)} y el máximo es 2 MB. Achicala o elegí otra.`;
  }
  return null;
}

export function traducirErrorImagen(error: HttpErrorResponse, accion: 'subir' | 'eliminar'): ErrorTraducido {
  const titulo = accion === 'subir' ? 'No pudimos subir la imagen' : 'No pudimos eliminar la imagen';
  if (error.status === 0) {
    return { titulo, detalle: 'No pudimos conectarnos con el servidor. Revisá tu conexión e intentá de nuevo.' };
  }
  if (error.status === 404) {
    return { titulo, detalle: 'El producto ya no existe. Puede que lo hayan dado de baja.' };
  }
  if (error.status === 413) {
    return { titulo, detalle: 'La imagen es demasiado pesada. El máximo es 2 MB.' };
  }
  const cuerpo = error.error as Partial<ApiError> | null;
  const mensaje = typeof cuerpo?.message === 'string' ? cuerpo.message : '';
  if (error.status >= 500) {
    return { titulo, detalle: 'Ocurrió un error en el servidor. Intentá de nuevo en unos minutos.' };
  }
  return { titulo, detalle: mensaje || `El servidor respondió con el código ${error.status}.` };
}
