package com.BlandiArruti.E_commerce.pedido.dto.response;

import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.enums.TipoReembolso;

/**
 * Resultado de cancelar un pedido (RF04).
 *
 * @param montoReembolsado lo que se devolvió (o hay que devolver, si es MANUAL); 0 si no correspondía
 * @param mensaje          texto listo para mostrarle a quien canceló
 */
public record CancelacionPedidoResponse(
        Long idPedido,
        EstadoPedido estado,
        TipoReembolso reembolso,
        double montoReembolsado,
        String mensaje
) {}
