package com.BlandiArruti.E_commerce.producto.dto.request;

import jakarta.validation.constraints.NotNull;

/** Marca o desmarca un producto como novedad (PATCH /producto/{id}/novedad). */
public record NovedadRequest(
        @NotNull(message = "Indicá si el producto es novedad (true o false).")
        Boolean novedad
) {}
