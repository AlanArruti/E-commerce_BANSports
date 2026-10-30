// Datos geográficos del back (PaisResponse, ProvinciaResponse, CiudadResponse).
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
