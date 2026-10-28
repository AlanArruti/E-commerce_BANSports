package com.BlandiArruti.E_commerce.exception;

public class StockInsuficienteException extends EcommerceException {

    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }

    // Variante sin stock disponible
    public static StockInsuficienteException sinStock(Long idVariante) {
        return new StockInsuficienteException(
                "La variante con id " + idVariante + " no tiene stock disponible."
        );
    }

    // Stock disponible menor al solicitado
    public static StockInsuficienteException stockInsuficiente(Long idVariante, int disponible, int solicitado) {
        return new StockInsuficienteException(
                "Stock insuficiente para la variante id " + idVariante +
                ". Disponible: " + disponible + ", solicitado: " + solicitado + "."
        );
    }
}