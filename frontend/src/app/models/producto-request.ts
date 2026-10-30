// Cuerpo de POST /producto y PUT /producto/{id} (ProductoRequest). Todos los campos son obligatorios.
export interface ProductoRequest {
  nombre: string;
  descripcion: string;
  precioBase: number;
  categoriaId: number;
}
