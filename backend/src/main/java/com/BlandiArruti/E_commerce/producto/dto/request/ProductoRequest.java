package com.BlandiArruti.E_commerce.producto.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductoRequest(
        @NotBlank(message = "El nombre no puede estar vacío.")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
        String nombre,

        @NotBlank(message = "La descripción no puede estar vacía.")
        @Size(max = 250, message = "La descripción no puede superar los 250 caracteres.")
        String descripcion,

        @NotNull(message = "El precio base es obligatorio.")
        @Positive(message = "El precio base debe ser positivo.")
        Double precioBase,

        /** Porcentaje de descuento. null o ausente = sin oferta. */
        @Min(value = 1, message = "El descuento debe ser mayor a 0.")
        @Max(value = 99, message = "El descuento no puede ser 100% o más.")
        Integer descuento,

        @NotNull(message = "El id de categoría es obligatorio.")
        Long categoriaId
) {}
