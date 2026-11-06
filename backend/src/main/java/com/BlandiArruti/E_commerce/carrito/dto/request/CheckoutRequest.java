package com.BlandiArruti.E_commerce.carrito.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull(message = "Tenés que elegir una dirección de envío.")
        Long idDireccion,

        @NotNull(message = "El costo de envío no puede estar vacío.")
        @DecimalMin(value = "0.0", inclusive = true, message = "El costo de envío no puede ser negativo.")
        Double costoEnvio
) {}
