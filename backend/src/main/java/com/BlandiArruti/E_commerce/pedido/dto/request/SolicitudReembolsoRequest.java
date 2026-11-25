package com.BlandiArruti.E_commerce.pedido.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** RF05: el cliente pide el reembolso de un pedido entregado. */
public record SolicitudReembolsoRequest(
        @NotBlank(message = "Contanos por qué querés el reembolso.")
        @Size(min = 10, max = 500, message = "El motivo tiene que tener entre 10 y 500 caracteres.")
        String motivo
) {}
