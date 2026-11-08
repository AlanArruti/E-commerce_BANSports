package com.BlandiArruti.E_commerce.carrito.dto.request;

import com.BlandiArruti.E_commerce.cotizacion.dto.request.OpcionEnvioRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull(message = "Tenés que elegir una dirección de envío.")
        Long idDireccion,

        // Solo la opción elegida: el costo lo vuelve a cotizar el back para esa dirección.
        @NotNull(message = "Tenés que elegir una opción de envío.")
        @Valid
        OpcionEnvioRequest opcionEnvio
) {}
