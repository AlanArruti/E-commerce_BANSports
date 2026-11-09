package com.BlandiArruti.E_commerce.carrito.dto.request;

import com.BlandiArruti.E_commerce.cotizacion.dto.request.OpcionEnvioRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
        @NotNull(message = "Tenés que elegir una dirección de envío.")
        Long idDireccion,

        // Solo la opción elegida: el costo lo vuelve a cotizar el back para esa dirección.
        @NotNull(message = "Tenés que elegir una opción de envío.")
        @Valid
        OpcionEnvioRequest opcionEnvio,

        // Opcional: el cupón que el cliente cargó en el checkout.
        @Size(max = 30, message = "El código de cupón no puede superar los 30 caracteres.")
        String codigoCupon
) {}
