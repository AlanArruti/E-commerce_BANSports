package com.BlandiArruti.E_commerce.carrito.dto.request;

import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull(message = "Tenés que elegir una dirección de envío.")
        Long idDireccion
) {}
