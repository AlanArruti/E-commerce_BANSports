package com.BlandiArruti.E_commerce.cliente.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DireccionRequest(
        @NotBlank(message = "El nombre de la calle no puede estar vacío.")
        @Size(max = 100, message = "El nombre de la calle no puede superar los 100 caracteres.")
        String nombreCalle,

        @NotNull(message = "El número de calle es obligatorio.")
        @Positive(message = "El número de calle debe ser positivo.")
        Integer numeroCalle,

        @NotBlank(message = "El código postal no puede estar vacío.")
        @Size(max = 8, message = "El código postal no puede superar los 8 caracteres.")
        String codigoPostal,

        @NotNull(message = "El id de ciudad es obligatorio.")
        Long idCiudad
) {}
