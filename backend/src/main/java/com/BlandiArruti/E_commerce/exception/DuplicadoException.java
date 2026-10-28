package com.BlandiArruti.E_commerce.exception;

public class DuplicadoException extends EcommerceException {

    public DuplicadoException(String mensaje) {
        super(mensaje);
    }

    public static DuplicadoException email(String email) {
        return new DuplicadoException("El email '" + email + "' ya está registrado.");
    }

    public static DuplicadoException dni(String dni) {
        return new DuplicadoException("El DNI '" + dni + "' ya está registrado.");
    }

    public static DuplicadoException username(String username) {
        return new DuplicadoException("El username '" + username + "' ya está en uso.");
    }
}