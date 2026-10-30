// Cuerpo de POST /producto y PUT /producto/{id} (ProductoRequest).
// categoriaId es obligatorio; descuento es opcional (null = sin oferta).
export interface ProductoRequest {
  nombre: string;
  descripcion: string;
  precioBase: number;
  descuento: number | null;
  categoriaId: number;
}
