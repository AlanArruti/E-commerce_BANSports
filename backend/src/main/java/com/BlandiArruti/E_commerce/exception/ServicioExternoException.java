package com.BlandiArruti.E_commerce.exception;

/**
 * Un servicio externo (por ejemplo Google Maps) no está configurado o no respondió.
 * No es culpa de los datos del usuario: se responde 503 para que el front lo diga así.
 */
public class ServicioExternoException extends RuntimeException {

    public ServicioExternoException(String mensaje) {
        super(mensaje);
    }

    public ServicioExternoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
