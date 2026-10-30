import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

// Validadores propios. Cada uno devuelve una clave de error distinta
// para poder mostrar un mensaje específico (ver mensajes.ts).

const LETRAS = /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ' ]+$/;
const NUMEROS = /^[0-9]+$/;
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

/** Obligatorio, pero además no acepta solo espacios en blanco. */
export const requerido: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const valor = control.value;
  if (valor === null || valor === undefined || String(valor).trim() === '') {
    return { requerido: true };
  }
  return null;
};

/** Solo letras (con tildes y ñ), espacios y apóstrofes. */
export const soloLetras: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const valor = String(control.value ?? '').trim();
  if (valor === '' || LETRAS.test(valor)) {
    return null;
  }
  return { soloLetras: true };
};

/** Solo dígitos, sin puntos ni espacios. */
export const soloNumeros: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const valor = String(control.value ?? '');
  if (valor === '' || NUMEROS.test(valor)) {
    return null;
  }
  return { soloNumeros: true };
};

/** DNI argentino: 7 u 8 dígitos. Solo se evalúa si ya son todos números. */
export const largoDni: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const valor = String(control.value ?? '');
  if (valor === '' || !NUMEROS.test(valor)) {
    return null;
  }
  if (valor.length < 7 || valor.length > 8) {
    return { largoDni: { actual: valor.length } };
  }
  return null;
};

/**
 * Exige que el valor coincida con el de otro campo del mismo formulario.
 * El control que lo usa tiene que refrescarse cuando cambia el otro campo
 * (ver perfil-modal.ts).
 */
export const coincideCon = (campo: string): ValidatorFn => {
  return (control: AbstractControl): ValidationErrors | null => {
    const valor = String(control.value ?? '');
    const original = String(control.parent?.get(campo)?.value ?? '');
    if (valor === '' || valor === original) {
      return null;
    }
    return { noCoinciden: true };
  };
};

/**
 * Convierte un precio escrito a la argentina en número: "42.999,99" → 42999.99, "1500" → 1500.
 * Devuelve NaN si el formato no es válido.
 */
export function leerPrecio(texto: string): number {
  const limpio = texto.trim();
  // Miles con punto (opcional) y hasta dos decimales con coma.
  if (!/^(\d{1,3}(\.\d{3})+|\d+)(,\d{1,2})?$/.test(limpio)) {
    return NaN;
  }
  return Number(limpio.replace(/\./g, '').replace(',', '.'));
}

/** Precio en formato argentino y mayor a cero. Dos claves de error distintas para dos mensajes. */
export const precioValido: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const valor = String(control.value ?? '').trim();
  if (valor === '') {
    return null;
  }
  const precio = leerPrecio(valor);
  if (Number.isNaN(precio)) {
    return { precioFormato: true };
  }
  return precio > 0 ? null : { precioCero: true };
};

/** Descuento opcional: vacío = sin oferta; si se carga, entero entre 1 y 99. */
export const descuentoValido: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const valor = String(control.value ?? '').trim();
  if (valor === '') {
    return null;
  }
  if (!NUMEROS.test(valor)) {
    return { descuentoInvalido: true };
  }
  const numero = Number(valor);
  return numero >= 1 && numero <= 99 ? null : { descuentoInvalido: true };
};

/** Email con formato nombre@dominio.ext (más estricto que Validators.email). */
export const emailValido: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const valor = String(control.value ?? '').trim();
  if (valor === '' || EMAIL.test(valor)) {
    return null;
  }
  return { emailValido: true };
};
