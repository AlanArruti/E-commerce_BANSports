import { Producto } from './producto';
import { Variante } from './variante';

// Ítem del carrito (ItemCarritoResponse). La variante es null si el producto no tiene variantes.
export interface ItemCarrito {
  idItemCarrito: number;
  producto: Producto;
  variante: Variante | null;
  cantidad: number;
  subtotal: number;
}
