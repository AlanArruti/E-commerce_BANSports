package com.BlandiArruti.E_commerce.exception;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * El cupón no se puede usar en este pedido. Responde 400 (por EcommerceException) con un
 * mensaje pensado para mostrarle al cliente tal cual en el checkout.
 */
public class CuponInvalidoException extends EcommerceException {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public CuponInvalidoException(String mensaje) {
        super(mensaje);
    }

    public static CuponInvalidoException inexistente(String codigo) {
        return new CuponInvalidoException("No existe un cupón con el código " + codigo + ". Revisá cómo está escrito.");
    }

    public static CuponInvalidoException inactivo(String codigo) {
        return new CuponInvalidoException("El cupón " + codigo + " ya no está disponible.");
    }

    public static CuponInvalidoException noEmpezo(String codigo, LocalDate desde) {
        return new CuponInvalidoException("El cupón " + codigo + " todavía no está vigente: se puede usar desde el "
                + desde.format(FECHA) + ".");
    }

    public static CuponInvalidoException vencido(String codigo, LocalDate hasta) {
        return new CuponInvalidoException("El cupón " + codigo + " venció el " + hasta.format(FECHA) + ".");
    }

    public static CuponInvalidoException agotado(String codigo) {
        return new CuponInvalidoException("El cupón " + codigo + " ya se usó todas las veces permitidas.");
    }

    public static CuponInvalidoException montoMinimo(String codigo, double minimo, double subtotal) {
        return new CuponInvalidoException("El cupón " + codigo + " es para compras desde " + pesos(minimo)
                + " en productos. Te faltan " + pesos(minimo - subtotal) + ".");
    }

    /** $ 50.000 o $ 1.234,50: el mismo formato que usa el front. */
    private static String pesos(double monto) {
        boolean entero = Math.abs(monto - Math.rint(monto)) < 0.005;
        String patron = entero ? "%,.0f" : "%,.2f";
        return "$ " + String.format(Locale.forLanguageTag("es-AR"), patron, monto);
    }
}
