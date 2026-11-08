package com.BlandiArruti.E_commerce.pago.dto.request;

import com.BlandiArruti.E_commerce.enums.TipoFactura;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record IniciarPagoRequest(
        @NotBlank(message = "Elegí con qué pasarela pagar.")
        @Pattern(regexp = "[a-z0-9-]{1,30}", message = "La pasarela no es válida.")
        String pasarela,

        @NotNull(message = "El tipo de factura es obligatorio.")
        TipoFactura tipoFactura
) {}
