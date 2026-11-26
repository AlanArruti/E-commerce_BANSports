import { Variante } from './variante';
import { claveCombinacion, combinar, opcionesDe, sugeridosPara } from './opciones-variante';

function variante(atributos: Record<string, string>): Variante {
  return { id: 1, uuidVariante: 'u', atributos, stock: 1, activo: true, idProducto: 1, precio: null, precioFinal: 1, imagenUrl: null };
}

describe('opciones de variante', () => {
  it('combina todas las opciones con valores (2 × 2 = 4)', () => {
    const combinaciones = combinar([
      { clave: 'sabor', valores: ['Chocolate', 'Vainilla'] },
      { clave: 'peso', valores: ['1 kg', '2 kg'] }
    ]);

    expect(combinaciones).toEqual([
      { sabor: 'Chocolate', peso: '1 kg' },
      { sabor: 'Chocolate', peso: '2 kg' },
      { sabor: 'Vainilla', peso: '1 kg' },
      { sabor: 'Vainilla', peso: '2 kg' }
    ]);
  });

  it('sin opciones con valores queda una sola variante: el producto único', () => {
    expect(combinar([])).toEqual([{ presentacion: 'Única' }]);
    expect(combinar([{ clave: 'talle', valores: [] }])).toEqual([{ presentacion: 'Única' }]);
  });

  it('arma las opciones desde las variantes, sin repetir valores ni distinguir mayúsculas en la clave', () => {
    const opciones = opcionesDe([
      variante({ sabor: 'Chocolate', peso: '1 kg' }),
      variante({ Sabor: 'chocolate', peso: '2 kg' }),
      variante({ sabor: 'Vainilla' })
    ]);

    expect(opciones).toEqual([
      { clave: 'sabor', valores: ['Chocolate', 'Vainilla'] },
      { clave: 'peso', valores: ['1 kg', '2 kg'] }
    ]);
  });

  it('identifica una combinación sin importar orden ni mayúsculas', () => {
    expect(claveCombinacion({ sabor: 'Chocolate', peso: '1 kg' })).toBe(claveCombinacion({ Peso: '1 KG', sabor: 'chocolate' }));
  });

  it('no sugiere valores que ya están cargados', () => {
    expect(sugeridosPara({ clave: 'talle', valores: ['m', 'L'] })).toEqual(['S', 'XL']);
  });
});
