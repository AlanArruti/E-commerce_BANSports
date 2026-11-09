import { FormControl, FormGroup } from '@angular/forms';
import { mensajeDeError } from './mensajes';
import {
  coincideCon,
  descuentoValido,
  emailValido,
  largoDni,
  leerPrecio,
  precioValido,
  requerido,
  soloLetras,
  soloNumeros
} from './validadores';

/** Corre un validador sobre un valor suelto. */
const validar = (validador: (c: FormControl) => unknown, valor: unknown) => validador(new FormControl(valor));

describe('validadores', () => {
  it('requerido rechaza vacío y solo espacios', () => {
    expect(validar(requerido, '')).toEqual({ requerido: true });
    expect(validar(requerido, '   ')).toEqual({ requerido: true });
    expect(validar(requerido, null)).toEqual({ requerido: true });
    expect(validar(requerido, 'Ana')).toBeNull();
    expect(validar(requerido, 0)).toBeNull();
  });

  it('soloLetras acepta tildes, ñ y apóstrofes', () => {
    expect(validar(soloLetras, "María O'Connor Núñez")).toBeNull();
    expect(validar(soloLetras, 'Ana2')).toEqual({ soloLetras: true });
    expect(validar(soloLetras, '')).toBeNull(); // vacío lo controla requerido
  });

  it('soloNumeros no acepta puntos ni espacios', () => {
    expect(validar(soloNumeros, '30111222')).toBeNull();
    expect(validar(soloNumeros, '30.111.222')).toEqual({ soloNumeros: true });
  });

  it('largoDni pide 7 u 8 dígitos y avisa cuántos tiene', () => {
    expect(validar(largoDni, '1234567')).toBeNull();
    expect(validar(largoDni, '12345678')).toBeNull();
    expect(validar(largoDni, '123456')).toEqual({ largoDni: { actual: 6 } });
    expect(validar(largoDni, '12.345')).toBeNull(); // ese error lo da soloNumeros
  });

  it('coincideCon compara con otro campo del formulario', () => {
    const form = new FormGroup({
      contrasenia: new FormControl('secreta'),
      repetir: new FormControl('', coincideCon('contrasenia'))
    });
    form.controls.repetir.setValue('otra');
    expect(form.controls.repetir.errors).toEqual({ noCoinciden: true });
    form.controls.repetir.setValue('secreta');
    expect(form.controls.repetir.errors).toBeNull();
  });

  it('emailValido exige dominio con extensión', () => {
    expect(validar(emailValido, 'ana@mail.com')).toBeNull();
    expect(validar(emailValido, 'ana@mail')).toEqual({ emailValido: true });
    expect(validar(emailValido, 'ana mail.com')).toEqual({ emailValido: true });
  });

  describe('precios', () => {
    it('leerPrecio entiende el formato argentino', () => {
      expect(leerPrecio('42.999,99')).toBe(42999.99);
      expect(leerPrecio('1500')).toBe(1500);
      expect(leerPrecio('1.500')).toBe(1500);
      expect(leerPrecio('1,5')).toBe(1.5);
      expect(leerPrecio('42,999.99')).toBeNaN(); // formato yanqui
      expect(leerPrecio('12.34')).toBeNaN(); // el punto es solo para miles
      expect(leerPrecio('abc')).toBeNaN();
    });

    it('precioValido separa formato inválido de precio en cero', () => {
      expect(validar(precioValido, '42.999,99')).toBeNull();
      expect(validar(precioValido, '')).toBeNull(); // opcional
      expect(validar(precioValido, '12.34')).toEqual({ precioFormato: true });
      expect(validar(precioValido, '0')).toEqual({ precioCero: true });
    });

    it('descuentoValido acepta enteros de 1 a 99 o vacío', () => {
      expect(validar(descuentoValido, '')).toBeNull();
      expect(validar(descuentoValido, '15')).toBeNull();
      expect(validar(descuentoValido, '0')).toEqual({ descuentoInvalido: true });
      expect(validar(descuentoValido, '100')).toEqual({ descuentoInvalido: true });
      expect(validar(descuentoValido, '12,5')).toEqual({ descuentoInvalido: true });
    });
  });
});

describe('mensajeDeError', () => {
  it('devuelve el mensaje del primer error, con el detalle', () => {
    const dni = new FormControl('123456', [largoDni]);
    expect(mensajeDeError(dni, 'dni')).toBe('El DNI debe tener 7 u 8 números (ingresaste 6).');
  });

  it('cuenta cuántos caracteres faltan en la contraseña', () => {
    const control = new FormControl('abcd');
    control.setErrors({ minlength: { requiredLength: 6, actualLength: 4 } });
    expect(mensajeDeError(control, 'contrasenia')).toBe('La contraseña debe tener al menos 6 caracteres (te faltan 2).');
    control.setErrors({ minlength: { requiredLength: 6, actualLength: 5 } });
    expect(mensajeDeError(control, 'contrasenia')).toContain('te falta 1');
  });

  it('muestra el error que mandó el back pegado al campo', () => {
    const control = new FormControl('ana@mail.com');
    control.setErrors({ servidor: 'Ya existe una cuenta con ese email.' });
    expect(mensajeDeError(control, 'email')).toBe('Ya existe una cuenta con ese email.');
  });

  it('sin errores devuelve null', () => {
    expect(mensajeDeError(new FormControl('Ana'), 'nombre')).toBeNull();
  });
});
