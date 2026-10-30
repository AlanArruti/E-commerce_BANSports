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

export interface Direccion {
  id: number;
  uuidDireccion: string;
  nombreCalle: string;
  numeroCalle: number;
  codigoPostal: string;
  ciudad: Ciudad;
}
