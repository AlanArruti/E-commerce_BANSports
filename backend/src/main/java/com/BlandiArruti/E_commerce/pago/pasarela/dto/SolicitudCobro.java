package com.BlandiArruti.E_commerce.pago.pasarela.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lo que se le pide cobrar a una pasarela. No conoce las entidades (Pedido, Pago):
 * PagoService la arma y así las pasarelas no dependen del modelo (Bajo Acoplamiento).
 *
 * @param referencia nuestro id de Pago; la pasarela lo guarda y lo devuelve al consultar
 * @param urlVuelta  página del front a la que vuelve el cliente al terminar
 */
public record SolicitudCobro(
        String referencia,
        String descripcion,
        List<LineaCobro> lineas,
        String urlVuelta
) {

    public BigDecimal total() {
        return lineas.stream().map(LineaCobro::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Una línea del pedido: un producto con su cantidad y el precio total de la línea. */
    public record LineaCobro(String codigo, String titulo, int cantidad, BigDecimal monto) {}
}
