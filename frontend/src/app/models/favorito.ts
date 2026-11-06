import { Producto } from './producto';

// Respuesta de GET /cliente/{id}/favoritos (FavoritoResponse). No trae productos dados de baja.
export interface Favorito {
  producto: Producto;
  /** Fecha en que se guardó (ISO). La lista viene ordenada del más nuevo al más viejo. */
  fechaAgregado: string;
}
