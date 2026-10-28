package com.BlandiArruti.E_commerce.exception;

public class CredencialesInvalidasException extends EcommerceException {

    public CredencialesInvalidasException() {
        super("Credenciales inválidas. Verifique su email y contraseña.");
    }

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }

    public static CredencialesInvalidasException deCliente() {
        return new CredencialesInvalidasException();
    }

    public static CredencialesInvalidasException deAdministrador() {
        return new CredencialesInvalidasException();
    }
}