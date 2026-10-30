// Cuerpo de POST /cliente/{id}/direccion (DireccionRequest).
// La ciudad y la provincia las saca el back del placeId de Google Maps.
export interface DireccionRequest {
  nombreCalle: string;
  numeroCalle: number;
  codigoPostal: string;
  placeId: string;
  /** Id de la búsqueda (el mismo de las sugerencias). */
  sesion: string;
}
