// Reseña de un producto (ResenaResponse).
export interface Resena {
  id: number;
  /** Nombre e inicial del apellido: "María G.". */
  autor: string;
  /** De 1 a 5. */
  estrellas: number;
  comentario: string | null;
  fecha: string;
}

export type MotivoPermisoResena = 'OK' | 'SIN_SESION' | 'NO_ES_CLIENTE' | 'NO_LO_COMPRO' | 'YA_OPINO';

// Respuesta de GET /producto/{id}/resenas/permiso: si quien mira puede opinar y por qué no.
export interface PermisoResena {
  puedeOpinar: boolean;
  motivo: MotivoPermisoResena;
  /** La que ya dejó, si motivo es YA_OPINO. */
  resenaPropia: Resena | null;
}

// Cuerpo de POST /producto/{id}/resenas.
export interface ResenaRequest {
  estrellas: number;
  comentario: string | null;
}
