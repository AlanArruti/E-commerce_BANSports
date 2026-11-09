package com.BlandiArruti.E_commerce.cupon.dto.response;

import com.BlandiArruti.E_commerce.enums.TipoCupon;

/**
 * Respuesta de GET /cupones/validar: el cupón sirve para el carrito actual del cliente.
 * El descuento es una vista previa; el que vale es el que se calcula al crear el pedido.
 */
public record ValidacionCuponResponse(
        String codigo,
        TipoCupon tipo,
        Double valor,
        /** Subtotal de productos del carrito (sin envío), sobre el que se calcula. */
        Double subtotal,
        Double descuento
) {}
