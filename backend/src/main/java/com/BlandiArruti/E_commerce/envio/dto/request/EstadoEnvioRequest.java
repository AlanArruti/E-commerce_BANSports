package com.BlandiArruti.E_commerce.envio.dto.request;

import com.BlandiArruti.E_commerce.enums.EstadoEnvio;
import jakarta.validation.constraints.NotNull;

public record EstadoEnvioRequest(
        @NotNull(message = "El estado del envío es obligatorio.")
        EstadoEnvio estado
) {}
