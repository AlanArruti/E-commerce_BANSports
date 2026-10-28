package com.BlandiArruti.E_commerce.producto.dto.request;

import com.BlandiArruti.E_commerce.enums.OperacionStock;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockRequest(
        @NotNull(message = "La cantidad es obligatoria.")
        @Min(value = 1, message = "La cantidad debe ser al menos 1.")
        Integer cantidad,

        @NotNull(message = "La operación es obligatoria.")
        OperacionStock operacion
) {}
