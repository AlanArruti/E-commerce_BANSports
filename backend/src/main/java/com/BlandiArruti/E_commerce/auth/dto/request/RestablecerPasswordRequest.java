package com.BlandiArruti.E_commerce.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestablecerPasswordRequest(
        @NotBlank(message = "Falta el token del enlace.")
        @Size(max = 100, message = "El token no es válido.")
        String token,

        @NotBlank(message = "La contraseña nueva es obligatoria.")
        @Size(min = 6, max = 50, message = "La contraseña debe tener entre 6 y 50 caracteres.")
        String contraseniaNueva
) {}
