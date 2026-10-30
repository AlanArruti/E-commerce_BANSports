import { Categoria } from './categoria';
import { Variante } from './variante';

// Respuesta de GET /producto y GET /producto/{id} (ProductoResponse).
export interface Producto {
  id: number;
  uuidProducto: string;
  nombre: string;
  descripcion: string;
  precioBase: number;
  /** Porcentaje de descuento. null o 0 = sin oferta. */
  descuento: number | null;
  /** Precio base con el descuento ya aplicado. */
  precioFinal: number;
  activo: boolean;
  /** Ruta relativa de la imagen (/api/v1/producto/{id}/imagen), o null si no tiene. */
  imagenUrl: string | null;
  categoria: Categoria | null;
  variantes: Variante[];
}
