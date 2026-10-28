package com.BlandiArruti.E_commerce.administrador.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdministradorRequest(
        @NotBlank(message = "El username no puede estar vacío.")
        @Size(max = 50, message = "El username no puede superar los 50 caracteres.")
        String username,

        @NotBlank(message = "La contraseña no puede estar vacía.")
        @Size(min = 6, max = 50, message = "La contraseña debe tener entre 6 y 50 caracteres.")
        String contrasenia
) {}
