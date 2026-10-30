// Cuerpo de POST /cliente/{id}/direccion (DireccionRequest). Todos los campos son obligatorios.
export interface DireccionRequest {
  nombreCalle: string;
  numeroCalle: number;
  codigoPostal: string;
  idCiudad: number;
}
