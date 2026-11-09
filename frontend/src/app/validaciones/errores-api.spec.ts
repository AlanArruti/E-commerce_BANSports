import { HttpErrorResponse } from '@angular/common/http';
import {
  traducirErrorCarrito,
  traducirErrorCheckout,
  traducirErrorDireccion,
  traducirErrorVariante
} from './errores-api';

/** Error HTTP como los que arma Angular, con el cuerpo que manda el back (ErrorResponse). */
function errorDelBack(status: number, message?: string): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: message ? { status, message } : null });
}

const STOCK_INSUFICIENTE = 'Stock insuficiente para la variante id 3. Disponible: 4, solicitado: 10.';

describe('traductores de errores del back', () => {
  describe('errores generales (cualquier pantalla)', () => {
    it('sin conexión', () => {
      const traducido = traducirErrorCarrito(errorDelBack(0));
      expect(traducido.titulo).toBe('No pudimos conectarnos con el servidor');
      expect(traducido.general).toBeTrue();
    });

    it('error del servidor', () => {
      const traducido = traducirErrorVariante(errorDelBack(503));
      expect(traducido.titulo).toBe('Ocurrió un error en el servidor');
      expect(traducido.general).toBeTrue();
    });
  });

  describe('stock insuficiente', () => {
    it('en el carrito dice cuántas quedan, contando las que ya tiene', () => {
      expect(traducirErrorCarrito(errorDelBack(400, STOCK_INSUFICIENTE))).toEqual({
        titulo: 'No hay stock suficiente',
        detalle: 'Quedan 4 unidades de esta variante, contando las que ya tenés en el carrito.'
      });
    });

    it('al confirmar el pedido pide ajustar el carrito', () => {
      const traducido = traducirErrorCheckout(errorDelBack(400, STOCK_INSUFICIENTE));
      expect(traducido.titulo).toBe('No hay stock suficiente');
      expect(traducido.detalle).toContain('quedan 4 unidades');
    });

    it('en el panel, al restar stock, dice cuánto hay', () => {
      expect(traducirErrorVariante(errorDelBack(400, STOCK_INSUFICIENTE))).toEqual({
        titulo: 'Stock insuficiente',
        detalle: 'Esta variante tiene 4 unidades: no podés restar más que eso.'
      });
    });
  });

  describe('variantes (panel)', () => {
    it('404: la variante ya no existe', () => {
      expect(traducirErrorVariante(errorDelBack(404)).titulo).toBe('Esa variante ya no existe');
    });

    it('otro 400 muestra lo que dijo el back', () => {
      expect(traducirErrorVariante(errorDelBack(400, 'Ya existe una variante con esos atributos.'))).toEqual({
        titulo: 'Revisá los datos de la variante',
        detalle: 'Ya existe una variante con esos atributos.'
      });
    });

    it('sin mensaje del back, al menos dice el código', () => {
      expect(traducirErrorVariante(errorDelBack(418)).detalle).toBe('El servidor respondió con el código 418.');
    });
  });

  describe('checkout', () => {
    it('si cambiaron las opciones de envío, pide elegir de nuevo', () => {
      const traducido = traducirErrorCheckout(errorDelBack(400, 'La opción de envío elegida ya no está disponible.'));
      expect(traducido.titulo).toBe('Cambiaron las opciones de envío');
    });

    it('409: carrito vacío', () => {
      expect(traducirErrorCheckout(errorDelBack(409)).titulo).toBe('Tu carrito está vacío');
    });

    it('404 en la dirección: la borraron', () => {
      expect(traducirErrorCheckout(errorDelBack(404)).titulo).toBe('No pudimos usar esa dirección');
    });
  });

  describe('direcciones', () => {
    it('503: el buscador de localidades (Georef) no responde', () => {
      const traducido = traducirErrorDireccion(errorDelBack(503));
      expect(traducido.titulo).toBe('La búsqueda de localidades no está disponible');
      // A diferencia de otros 5xx, este sí se muestra en el formulario: no es el back el que está caído.
      expect(traducido.general).toBeUndefined();
    });

    it('404: la localidad no existe', () => {
      expect(traducirErrorDireccion(errorDelBack(404)).titulo).toBe('No encontramos esa localidad');
    });

    it('400 muestra el mensaje de validación del back', () => {
      expect(traducirErrorDireccion(errorDelBack(400, 'La calle es obligatoria.'))).toEqual({
        titulo: 'Revisá los datos de la dirección',
        detalle: 'La calle es obligatoria.'
      });
    });

    it('sin conexión usa el error general', () => {
      expect(traducirErrorDireccion(errorDelBack(0)).general).toBeTrue();
    });
  });
});
