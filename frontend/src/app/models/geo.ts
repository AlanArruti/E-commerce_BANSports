// Localidad del buscador del formulario de dirección (LocalidadResponse, sale de Georef).
export interface Localidad {
  /** Id de Georef (8 dígitos). */
  id: string;
  nombre: string;
  /** Departamento o partido: distingue localidades con el mismo nombre. */
  departamento: string | null;
  provincia: string;
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
