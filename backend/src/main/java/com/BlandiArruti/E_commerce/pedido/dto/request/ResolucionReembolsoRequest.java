package com.BlandiArruti.E_commerce.pedido.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** RF05: el admin aprueba o rechaza el reembolso pedido. Al rechazar, la respuesta es obligatoria. */
public record ResolucionReembolsoRequest(
        @NotNull(message = "Indicá si aprobás el reembolso.")
        Boolean aprobar,

        @Size(max = 500, message = "La respuesta no puede superar los 500 caracteres.")
        String respuesta
) {}
