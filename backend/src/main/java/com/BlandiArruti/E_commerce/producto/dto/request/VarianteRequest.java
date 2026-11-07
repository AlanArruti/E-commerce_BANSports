package com.BlandiArruti.E_commerce.producto.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Map;

public record VarianteRequest(
        @NotEmpty(message = "Los atributos no pueden estar vacíos.")
        Map<String, String> atributos,

        @NotNull(message = "El stock es obligatorio.")
        @Min(value = 0, message = "El stock no puede ser negativo.")
        Integer stock,

        /** Opcional: vacío = la variante cuesta lo mismo que el producto. */
        @Positive(message = "El precio de la variante tiene que ser mayor a $ 0.")
        Double precio
) {}
