import { Variante, etiquetaAtributo } from './variante';

/**
 * Opciones de un producto (sabor, peso, talle…) y las combinaciones que salen de ellas.
 * El back sigue guardando cada variante como un mapa { sabor: 'Chocolate', peso: '1 kg' }:
 * las opciones se arman en el front a partir de esos mapas.
 */
export interface Opcion {
  /** Clave que se guarda en el back, en minúscula: 'sabor'. */
  clave: string;
  /** Valores en el orden en que se cargaron: ['Chocolate', 'Vainilla']. */
  valores: string[];
}

/** Opciones que se ofrecen con un clic, con valores sugeridos. */
export const OPCIONES_SUGERIDAS: { clave: string; sugeridos: string[] }[] = [
  { clave: 'sabor', sugeridos: ['Chocolate', 'Vainilla', 'Frutilla', 'Cookies', 'Neutro'] },
  { clave: 'peso', sugeridos: ['300 g', '500 g', '1 kg', '2 kg', '3 kg'] },
  { clave: 'talle', sugeridos: ['S', 'M', 'L', 'XL'] },
  { clave: 'color', sugeridos: ['Negro', 'Blanco', 'Rojo', 'Azul', 'Gris'] }
];

/** Atributo que se usa cuando el producto no tiene opciones: el back exige al menos uno. */
export const ATRIBUTOS_PRODUCTO_UNICO: Record<string, string> = { presentacion: 'Única' };

export const LARGO_OPCION = 50;

/** 'Talle ' → 'talle'. Así "Sabor" y "sabor" son la misma opción. */
export function claveDeOpcion(nombre: string): string {
  return nombre.trim().toLowerCase();
}

export function nombreDeOpcion(clave: string): string {
  return etiquetaAtributo(clave);
}

export function mismoTexto(a: string, b: string): boolean {
  return a.trim().toLowerCase() === b.trim().toLowerCase();
}

/** Valores sugeridos de una opción que todavía no se cargaron. */
export function sugeridosPara(opcion: Opcion): string[] {
  const sugeridos = OPCIONES_SUGERIDAS.find((o) => o.clave === opcion.clave)?.sugeridos ?? [];
  return sugeridos.filter((s) => !opcion.valores.some((v) => mismoTexto(v, s)));
}

/** Las opciones que ya usan las variantes del producto, con todos sus valores (sin repetir). */
export function opcionesDe(variantes: Variante[]): Opcion[] {
  const opciones: Opcion[] = [];
  for (const variante of variantes) {
    for (const [claveOriginal, valor] of Object.entries(variante.atributos)) {
      const clave = claveDeOpcion(claveOriginal);
      let opcion = opciones.find((o) => o.clave === clave);
      if (!opcion) {
        opcion = { clave, valores: [] };
        opciones.push(opcion);
      }
      if (!opcion.valores.some((v) => mismoTexto(v, valor))) {
        opcion.valores.push(valor.trim());
      }
    }
  }
  // Presentación primero, como en el resto de la tienda.
  return opciones.sort((a, b) => Number(b.clave === 'presentacion') - Number(a.clave === 'presentacion'));
}

/**
 * Todas las combinaciones de las opciones que tienen valores.
 * 2 sabores × 2 pesos = 4 mapas. Sin opciones con valores, una sola: el producto único.
 */
export function combinar(opciones: Opcion[]): Record<string, string>[] {
  const conValores = opciones.filter((o) => o.valores.length > 0);
  if (conValores.length === 0) {
    return [{ ...ATRIBUTOS_PRODUCTO_UNICO }];
  }
  return conValores.reduce<Record<string, string>[]>(
    (combinaciones, opcion) =>
      combinaciones.flatMap((parcial) => opcion.valores.map((valor) => ({ ...parcial, [opcion.clave]: valor }))),
    [{}]
  );
}

/** Identifica una combinación sin importar el orden ni las mayúsculas: 'peso=1 kg|sabor=chocolate'. */
export function claveCombinacion(atributos: Record<string, string>): string {
  return Object.entries(atributos)
    .map(([clave, valor]) => `${claveDeOpcion(clave)}=${valor.trim().toLowerCase()}`)
    .sort()
    .join('|');
}
