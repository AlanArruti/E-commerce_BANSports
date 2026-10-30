// Respuesta de DELETE /categorias/{id} (EliminacionResponse).
// Sin ?confirmar=true el back no borra nada: avisa qué productos quedarían sin categoría.
export interface EliminacionResponse {
  mensaje: string;
  /** Productos afectados, con el formato "id 3 — Whey Protein Isolate". */
  aviso: string[];
}
