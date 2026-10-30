// Cuerpo de POST /cliente/{id}/direccion (DireccionRequest).
// La ciudad y la provincia las saca el back de la localidad elegida (Georef).
export interface DireccionRequest {
  nombreCalle: string;
  numeroCalle: number;
  codigoPostal: string;
  /** Id de Georef de la localidad elegida en el buscador. */
  idLocalidad: string;
}
