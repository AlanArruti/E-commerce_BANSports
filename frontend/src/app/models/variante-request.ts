// Cuerpo de POST /producto/{id}/variante y PUT /producto/{id}/variante/{idVariante} (VarianteRequest).
export interface VarianteRequest {
  /** Pares libres clave–valor: { sabor: 'Chocolate', peso: '1 kg' }. Al menos uno. */
  atributos: Record<string, string>;
  /** Stock inicial (≥ 0). */
  stock: number;
  /** Precio propio de la variante, sin descuento. null = cuesta lo mismo que el producto. */
  precio: number | null;
}

// Cuerpo de PATCH /producto/{id}/variante/{idVariante}/stock (StockRequest).
export type OperacionStock = 'AGREGAR' | 'REDUCIR';

export interface StockRequest {
  operacion: OperacionStock;
  /** Unidades a sumar o restar (> 0). */
  cantidad: number;
}
