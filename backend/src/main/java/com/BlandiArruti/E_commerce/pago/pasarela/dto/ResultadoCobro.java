package com.BlandiArruti.E_commerce.pago.pasarela.dto;

import com.BlandiArruti.E_commerce.enums.EstadoPago;

import java.math.BigDecimal;

/**
 * Estado de un cobro según la pasarela, ya traducido a nuestro idioma (Adapter).
 *
 * @param referencia la que mandamos al crearlo (nuestro id de Pago); sirve para verificar
 *                   que el cobro consultado es de ese pago y no de otro
 */
public record ResultadoCobro(
        String idExterno,
        EstadoPago estado,
        BigDecimal montoPagado,
        String referencia
) {}
