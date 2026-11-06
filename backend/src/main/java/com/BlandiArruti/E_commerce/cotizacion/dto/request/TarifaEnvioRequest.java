package com.BlandiArruti.E_commerce.cotizacion.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TarifaEnvioRequest(
        @Size(max = 50, message = "La provincia no puede superar los 50 caracteres.")
        String provincia,

        @NotNull(message = "El precio no puede estar vacío.")
        @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo.")
        Double precio,

        @NotNull(message = "El plazo mínimo no puede estar vacío.")
        @Min(value = 0, message = "El plazo mínimo no puede ser negativo.")
        @Max(value = 90, message = "El plazo mínimo no puede superar los 90 días.")
        Integer plazoMinimoDias,

        @NotNull(message = "El plazo máximo no puede estar vacío.")
        @Min(value = 0, message = "El plazo máximo no puede ser negativo.")
        @Max(value = 90, message = "El plazo máximo no puede superar los 90 días.")
        Integer plazoMaximoDias
) {}
