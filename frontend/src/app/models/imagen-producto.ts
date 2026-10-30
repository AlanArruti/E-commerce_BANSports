// Respuesta de POST /producto/{id}/imagen (ImagenProductoResponse).
export interface ImagenProducto {
  idProducto: number;
  nombreArchivo: string;
  contentType: string;
  tamanioBytes: number;
  /** Ruta relativa para el <img src> (/api/v1/producto/{id}/imagen). */
  url: string;
}
