// Formato de error que devuelve el back (ErrorResponse / GlobalExceptionHandler).
export interface ApiError {
  timestamp?: string;
  status: number;
  error: string;
  message: string;
  path?: string;
}
