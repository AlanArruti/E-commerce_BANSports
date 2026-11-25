package com.BlandiArruti.E_commerce.cliente.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambioPasswordRequest(
        /** Obligatoria salvo que la cuenta se haya creado con Google o GitHub y todavía no tenga contraseña propia. */
        String contraseniaActual,

        @NotBlank(message = "La contraseña nueva es obligatoria.")
        @Size(min = 6, max = 50, message = "La contraseña debe tener entre 6 y 50 caracteres.")
        String contraseniaNueva
) {}
