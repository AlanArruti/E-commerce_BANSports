import { AbstractControl, ValidationErrors } from '@angular/forms';

/*
 * Todos los mensajes de validación en un solo lugar.
 * Regla del equipo: cada error tiene su propio mensaje, que dice qué está mal
 * y cómo corregirlo. Nunca "Campo inválido" ni "Error".
 *
 * Cada campo es un objeto { claveDeError: (detalle) => mensaje }.
 * El orden importa: se muestra el primer error que aparezca en la lista.
 */
type Mensajes = Record<string, (detalle: any, valor: string) => string>;

const MSJ_EMAIL_FORMATO = 'El email no es válido. Debe tener el formato nombre@dominio.com.';

export const MENSAJES: Record<string, Mensajes> = {
  nombre: {
    requerido: () => 'Ingresá tu nombre.',
    soloLetras: () => 'El nombre solo puede tener letras y espacios.',
    maxlength: (d) => `El nombre puede tener hasta ${d.requiredLength} caracteres (tiene ${d.actualLength}).`
  },
  apellido: {
    requerido: () => 'Ingresá tu apellido.',
    soloLetras: () => 'El apellido solo puede tener letras y espacios.',
    maxlength: (d) => `El apellido puede tener hasta ${d.requiredLength} caracteres (tiene ${d.actualLength}).`
  },
  dni: {
    requerido: () => 'Ingresá tu DNI.',
    soloNumeros: () => 'El DNI solo puede tener números, sin puntos ni espacios.',
    largoDni: (d) => `El DNI debe tener 7 u 8 números (ingresaste ${d.actual}).`
  },
  email: {
    requerido: () => 'Ingresá tu email.',
    emailValido: () => MSJ_EMAIL_FORMATO,
    maxlength: (d) => `El email puede tener hasta ${d.requiredLength} caracteres (tiene ${d.actualLength}).`
  },
  contrasenia: {
    requerido: () => 'Ingresá una contraseña.',
    minlength: (d) => {
      const faltan = d.requiredLength - d.actualLength;
      return `La contraseña debe tener al menos ${d.requiredLength} caracteres (te ${faltan === 1 ? 'falta 1' : `faltan ${faltan}`}).`;
    },
    maxlength: (d) => `La contraseña puede tener hasta ${d.requiredLength} caracteres (tiene ${d.actualLength}).`
  },
  terminos: {
    required: () => 'Tenés que aceptar los Términos y condiciones para crear la cuenta.'
  },
  loginEmail: {
    requerido: () => 'Ingresá el email con el que te registraste.',
    emailValido: () => MSJ_EMAIL_FORMATO
  },
  loginUsuario: {
    requerido: () => 'Ingresá tu usuario de administrador.'
  },
  loginContrasenia: {
    requerido: () => 'Ingresá tu contraseña.'
  },
  categoriaNombre: {
    requerido: () => 'Ingresá el nombre de la categoría.',
    maxlength: (d) => `El nombre puede tener hasta ${d.requiredLength} caracteres (tiene ${d.actualLength}).`,
    repetido: (_d, valor) => `Ya existe una categoría llamada "${valor.trim()}".`
  },
  pais: {
    required: () => 'Elegí el país.'
  },
  provincia: {
    required: () => 'Elegí la provincia.'
  },
  ciudad: {
    required: () => 'Elegí la ciudad.'
  },
  calle: {
    requerido: () => 'Ingresá la calle.',
    maxlength: (d) => `La calle puede tener hasta ${d.requiredLength} caracteres (tiene ${d.actualLength}).`
  },
  numeroCalle: {
    requerido: () => 'Ingresá la altura de la calle.',
    soloNumeros: () => 'La altura solo puede tener números, sin espacios ni letras (por ejemplo 1234).',
    min: () => 'La altura tiene que ser mayor a 0.'
  },
  codigoPostal: {
    requerido: () => 'Ingresá el código postal.',
    maxlength: (d) => `El código postal puede tener hasta ${d.requiredLength} caracteres (tiene ${d.actualLength}).`
  }
};

/** Devuelve el mensaje del primer error del control, o null si no hay error. */
export function mensajeDeError(control: AbstractControl | null, campo: string): string | null {
  if (!control || !control.errors) {
    return null;
  }
  const errores: ValidationErrors = control.errors;
  const mensajes = MENSAJES[campo] ?? {};
  for (const clave of Object.keys(mensajes)) {
    if (errores[clave] !== undefined) {
      return mensajes[clave](errores[clave], String(control.value ?? ''));
    }
  }
  // Error del back pegado al campo (ver errores-api.ts)
  if (typeof errores['servidor'] === 'string') {
    return errores['servidor'];
  }
  return null;
}

export const MSJ_PRECIO_RANGO = 'El precio mínimo no puede ser mayor que el máximo.';
