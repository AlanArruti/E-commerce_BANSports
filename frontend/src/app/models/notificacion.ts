// Respuesta de GET /cliente/{id}/notificaciones (NotificacionResponse). RF02.
export type TipoNotificacion = 'PEDIDO' | 'OFERTA';

export interface Notificacion {
  id: number;
  tipo: TipoNotificacion;
  titulo: string;
  mensaje: string;
  /** Id del pedido (PEDIDO) o del producto (OFERTA). */
  idReferencia: number | null;
  leida: boolean;
  /** Fecha ISO. La lista viene de la más nueva a la más vieja. */
  fechaCreacion: string;
}

export interface NoLeidas {
  cantidad: number;
}

/** Adónde lleva cada notificación al tocarla. */
export function enlaceDe(notificacion: Notificacion): string[] | null {
  if (notificacion.idReferencia === null) {
    return null;
  }
  return notificacion.tipo === 'PEDIDO'
    ? ['/perfil/pedidos', String(notificacion.idReferencia)]
    : ['/productos', String(notificacion.idReferencia)];
}
