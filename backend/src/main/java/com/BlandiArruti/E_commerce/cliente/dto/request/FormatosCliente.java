package com.BlandiArruti.E_commerce.cliente.dto.request;

/**
 * Formatos de los datos del cliente. Son las mismas reglas que valida el front
 * (validaciones/validadores.ts), repetidas acá para que no se salteen llamando a la API directo.
 */
final class FormatosCliente {

    /** Letras (con tildes, diéresis y ñ), espacios y apóstrofes. */
    static final String SOLO_LETRAS = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ' ]+$";
    /** DNI argentino: 7 u 8 números, sin puntos. */
    static final String DNI = "^\\d{7,8}$";
    /** nombre@dominio.ext: @Email solo acepta "juan@test", sin extensión. */
    static final String EMAIL = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$";

    static final String MENSAJE_EMAIL = "El email no es válido. Debe tener el formato nombre@dominio.com.";

    private FormatosCliente() {
    }
}
