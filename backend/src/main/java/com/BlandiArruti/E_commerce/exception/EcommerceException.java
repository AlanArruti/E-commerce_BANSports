package com.BlandiArruti.E_commerce.exception;

public class EcommerceException extends RuntimeException {

    public EcommerceException(String mensaje) {
        super(mensaje);
    }

    public EcommerceException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}