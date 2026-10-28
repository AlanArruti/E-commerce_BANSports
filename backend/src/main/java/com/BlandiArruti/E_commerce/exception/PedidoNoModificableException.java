package com.BlandiArruti.E_commerce.exception;

import com.BlandiArruti.E_commerce.enums.EstadoPedido;

public class PedidoNoModificableException extends EcommerceException {

    public PedidoNoModificableException(String mensaje) {
        super(mensaje);
    }

    // Pedido en estado que no permite modificaciones
    public static PedidoNoModificableException porEstado(Long idPedido, EstadoPedido estado) {
        return new PedidoNoModificableException(
                "El pedido con id " + idPedido + " no puede modificarse porque está en estado: " + estado + "."
        );
    }

    // Intento de cancelar un pedido ya cancelado o entregado
    public static PedidoNoModificableException noCancelable(Long idPedido, EstadoPedido estado) {
        return new PedidoNoModificableException(
                "El pedido con id " + idPedido + " no puede cancelarse porque está en estado: " + estado + "."
        );
    }
}