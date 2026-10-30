// Sugerencia del autocompletado de direcciones (SugerenciaDireccionResponse).
export interface SugerenciaDireccion {
  placeId: string;
  /** "Av. Corrientes 1234" */
  principal: string;
  /** "CABA, Argentina" */
  secundario: string | null;
}

// Dirección elegida, desarmada en partes (LugarResponse). Altura y CP pueden faltar.
export interface Lugar {
  placeId: string;
  direccionCompleta: string;
  nombreCalle: string | null;
  numeroCalle: number | null;
  codigoPostal: string | null;
  ciudad: string | null;
  provincia: string | null;
  pais: string | null;
  codigoPais: string | null;
}

// Datos geográficos que vienen dentro de una dirección guardada (PaisResponse, ProvinciaResponse, CiudadResponse).
export interface Pais {
  idPais: number;
  nombrePais: string;
}

export interface Provincia {
  idProvincia: number;
  nombreProvincia: string;
  pais: Pais;
}

export interface Ciudad {
  idCiudad: number;
  nombreCiudad: string;
  provincia: Provincia;
}
