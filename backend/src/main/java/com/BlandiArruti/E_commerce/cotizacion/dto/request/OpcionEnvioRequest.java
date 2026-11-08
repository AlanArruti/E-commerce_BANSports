package com.BlandiArruti.E_commerce.cotizacion.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * La opción de envío que eligió el cliente, tal como vino en la cotización.
 * No lleva precio: el back vuelve a cotizar la dirección y toma el precio de ahí.
 */
public record OpcionEnvioRequest(
        @NotBlank(message = "Falta el tipo de entrega del envío.")
        String tipoEntrega,

        @NotBlank(message = "Falta el servicio de envío.")
        String servicio
) {}
