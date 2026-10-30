export type Orden = 'relevantes' | 'nuevos' | 'precio-asc' | 'precio-desc' | 'nombre-asc' | 'nombre-desc';

export interface OpcionOrden {
  valor: Orden;
  etiqueta: string;
  sortBy: 'id' | 'nombre' | 'precio';
  sortDir: 'asc' | 'desc';
}

// Opciones del selector "Ordenar por" y cómo se traducen a los parámetros del back.
export const ORDENES: OpcionOrden[] = [
  { valor: 'relevantes', etiqueta: 'Más relevantes', sortBy: 'id', sortDir: 'asc' },
  { valor: 'precio-asc', etiqueta: 'Precio: menor a mayor', sortBy: 'precio', sortDir: 'asc' },
  { valor: 'precio-desc', etiqueta: 'Precio: mayor a menor', sortBy: 'precio', sortDir: 'desc' },
  { valor: 'nombre-asc', etiqueta: 'Nombre: A–Z', sortBy: 'nombre', sortDir: 'asc' },
  { valor: 'nombre-desc', etiqueta: 'Nombre: Z–A', sortBy: 'nombre', sortDir: 'desc' }
];

// Solo para el panel: lo último que se cargó, arriba. No se ofrece en el catálogo del cliente.
export const ORDEN_NUEVOS: OpcionOrden = { valor: 'nuevos', etiqueta: 'Más nuevos primero', sortBy: 'id', sortDir: 'desc' };

// Filtros del catálogo. Salen de la URL (/productos?search=whey&categoria=2...).
export interface FiltrosCatalogo {
  search: string;
  categoriaId: number | null;
  precioMin: number | null;
  precioMax: number | null;
  orden: Orden;
  /** Página para el back, empezando en 0 (en la URL se muestra desde 1). */
  pagina: number;
  tamanio: number;
}
