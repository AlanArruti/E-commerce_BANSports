// Respuesta paginada del back (PageResponse<T>). La página empieza en 0.
export interface Pagina<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}
