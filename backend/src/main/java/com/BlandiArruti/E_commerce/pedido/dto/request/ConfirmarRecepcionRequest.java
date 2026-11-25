package com.BlandiArruti.E_commerce.pedido.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConfirmarRecepcionRequest(
        @NotNull(message = "Indicá si recibiste el pedido.")
        Boolean recibido,

        /** Opcional, solo cuando no llegó: qué pasó (ej. "no vino nadie", "lo recibió un vecino"). */
        @Size(max = 500, message = "El comentario no puede superar los 500 caracteres.")
        String comentario
) {}
