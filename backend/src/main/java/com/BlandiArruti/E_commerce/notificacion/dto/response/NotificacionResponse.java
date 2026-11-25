package com.BlandiArruti.E_commerce.notificacion.dto.response;

import com.BlandiArruti.E_commerce.enums.TipoNotificacion;

import java.time.LocalDateTime;

public record NotificacionResponse(
        Long id,
        TipoNotificacion tipo,
        String titulo,
        String mensaje,
        /** Id del pedido (PEDIDO) o del producto (OFERTA). */
        Long idReferencia,
        boolean leida,
        LocalDateTime fechaCreacion
) {}
