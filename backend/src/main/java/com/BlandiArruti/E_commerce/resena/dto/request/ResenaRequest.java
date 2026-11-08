package com.BlandiArruti.E_commerce.resena.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResenaRequest(
        @NotNull(message = "Las estrellas son obligatorias.")
        @Min(value = 1, message = "Las estrellas tienen que ser entre 1 y 5.")
        @Max(value = 5, message = "Las estrellas tienen que ser entre 1 y 5.")
        Integer estrellas,

        @Size(max = 500, message = "El comentario no puede superar los 500 caracteres.")
        String comentario
) {}
