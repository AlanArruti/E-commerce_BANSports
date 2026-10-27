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

/** Email con formato nombre@dominio.ext (más estricto que Validators.email). */
export const emailValido: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const valor = String(control.value ?? '').trim();
  if (valor === '' || EMAIL.test(valor)) {
    return null;
  }
  return { emailValido: true };
};
