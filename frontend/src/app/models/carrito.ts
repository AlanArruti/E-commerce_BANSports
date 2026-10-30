import { ItemCarrito } from './item-carrito';

// Respuesta de GET /cliente/{id}/carrito (CarritoResponse). Cada cliente tiene un solo carrito.
export interface Carrito {
  id: number;
  uuidCarrito: string;
  items: ItemCarrito[];
  total: number;
}
