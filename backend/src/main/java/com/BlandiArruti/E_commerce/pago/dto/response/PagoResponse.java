package com.BlandiArruti.E_commerce.pago.dto.response;

import com.BlandiArruti.E_commerce.enums.EstadoPago;
import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.enums.TipoFactura;

import java.time.LocalDateTime;

/** Un intento de pago, con el estado del pedido para que el front sepa qué mostrar. */
public record PagoResponse(
        Long id,
        Long pedidoId,
        String pasarela,
        EstadoPago estado,
        TipoFactura tipoFactura,
        Double monto,
        Double montoPagado,
        EstadoPedido estadoPedido,
        LocalDateTime fechaCreacion
) {}
