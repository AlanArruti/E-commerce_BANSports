package com.BlandiArruti.E_commerce.cliente.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambioPasswordRequest(
        @NotBlank(message = "La contraseña actual es obligatoria.")
        String contraseniaActual,

        @NotBlank(message = "La contraseña nueva es obligatoria.")
        @Size(min = 6, max = 50, message = "La contraseña debe tener entre 6 y 50 caracteres.")
        String contraseniaNueva
) {}
