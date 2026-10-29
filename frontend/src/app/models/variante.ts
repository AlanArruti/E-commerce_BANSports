// Variante de un producto (VarianteResponse). Los atributos son libres: { sabor: 'Chocolate', peso: '1 kg' }.
export interface Variante {
  id: number;
  uuidVariante: string;
  atributos: Record<string, string>;
  stock: number;
  activo: boolean;
  idProducto: number;
}
