// Variante de un producto (VarianteResponse). Los atributos son libres: { sabor: 'Chocolate', peso: '1 kg' }.
export interface Variante {
  id: number;
  uuidVariante: string;
  atributos: Record<string, string>;
  stock: number;
  activo: boolean;
  idProducto: number;
}

/** A partir de cuántas unidades se avisa que queda poco stock. */
export const STOCK_BAJO = 5;

/** Nombres para mostrar de los atributos más comunes. El resto se muestra con mayúscula inicial. */
const ETIQUETAS_ATRIBUTO: Record<string, string> = {
  presentacion: 'Presentación',
  sabor: 'Sabor',
  talla: 'Talle',
  talle: 'Talle',
  color: 'Color',
  peso: 'Peso'
};

export function etiquetaAtributo(clave: string): string {
  return ETIQUETAS_ATRIBUTO[clave.toLowerCase()] ?? clave.charAt(0).toUpperCase() + clave.slice(1);
}

/**
 * Atributos de la variante en orden de lectura.
 * El back los guarda en jsonb y no respeta el orden: la presentación va siempre primero.
 */
export function atributosOrdenados(variante: Variante): [string, string][] {
  return Object.entries(variante.atributos).sort(
    ([a], [b]) => Number(b === 'presentacion') - Number(a === 'presentacion')
  );
}
