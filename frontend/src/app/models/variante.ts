import type { Producto } from './producto';

// Variante de un producto (VarianteResponse). Los atributos son libres: { sabor: 'Chocolate', peso: '1 kg' }.
export interface Variante {
  id: number;
  uuidVariante: string;
  atributos: Record<string, string>;
  stock: number;
  activo: boolean;
  idProducto: number;
  /** Precio propio cargado en el panel, sin descuento. null = cuesta lo mismo que el producto. */
  precio: number | null;
  /** Lo que paga el cliente por una unidad: su precio (o el del producto) con la oferta aplicada. */
  precioFinal: number;
  /** Foto propia de la variante (ej. el sabor). null = se muestra la del producto. */
  imagenUrl: string | null;
}

// ── Precios (SCRUM-154): cada variante puede tener su propio precio ──────────

/** Lo que paga el cliente por una unidad de esa variante (o del producto, si no tiene variante). */
export function precioFinalDe(producto: Producto, variante: Variante | null | undefined): number {
  return variante?.precioFinal ?? producto.precioFinal ?? producto.precioBase;
}

/** Precio de lista, sin la oferta: es el que se muestra tachado cuando hay descuento. */
export function precioListaDe(producto: Producto, variante: Variante | null | undefined): number {
  return variante?.precio ?? producto.precioBase;
}

/**
 * Precio más bajo y más alto entre las variantes activas, para mostrar "Desde $X" cuando difieren.
 * Sin variantes, los dos son el precio del producto.
 */
export function rangoDePrecios(producto: Producto): { minimo: number; maximo: number; listaMinimo: number } {
  const activas = (producto.variantes ?? []).filter((v) => v.activo);
  if (activas.length === 0) {
    const unico = precioFinalDe(producto, null);
    return { minimo: unico, maximo: unico, listaMinimo: producto.precioBase };
  }
  const masBarata = activas.reduce((min, v) => (precioFinalDe(producto, v) < precioFinalDe(producto, min) ? v : min));
  const finales = activas.map((v) => precioFinalDe(producto, v));
  return {
    minimo: precioFinalDe(producto, masBarata),
    maximo: Math.max(...finales),
    listaMinimo: precioListaDe(producto, masBarata)
  };
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
