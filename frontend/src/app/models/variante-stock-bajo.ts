// Respuesta de GET /admin/variantes/stock-bajo (VarianteStockBajoResponse).
export interface VarianteStockBajo {
  idVariante: number;
  idProducto: number;
  nombreProducto: string;
  atributos: Record<string, string>;
  stock: number;
}
