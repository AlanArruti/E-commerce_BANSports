// Cuerpo de POST /cliente/{id}/carrito/items. Si el ítem ya está en el carrito, el back suma la cantidad.
export interface ItemCarritoRequest {
  idProducto: number;
  idVariante: number | null;
  cantidad: number;
}
