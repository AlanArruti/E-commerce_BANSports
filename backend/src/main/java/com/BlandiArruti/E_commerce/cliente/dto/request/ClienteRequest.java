package com.BlandiArruti.E_commerce.cliente.dto.request;

import jakarta.validation.constraints.*;

public record ClienteRequest(
        @NotBlank(message = "El nombre no puede estar vacío.")
        @Size(max = 50, message = "El nombre no puede superar los 50 caracteres.")
        String nombre,

        @NotBlank(message = "El apellido no puede estar vacío.")
        @Size(max = 50, message = "El apellido no puede superar los 50 caracteres.")
        String apellido,

        @NotBlank(message = "El DNI no puede estar vacío.")
        @Size(max = 20, message = "El DNI no puede superar los 20 caracteres.")
        String dni,

        @NotBlank(message = "El email no puede estar vacío.")
        @Email(message = "El email no tiene formato válido.")
        @Size(max = 100, message = "El email no puede superar los 100 caracteres.")
        String email,

        @NotBlank(message = "La contraseña no puede estar vacía.")
        @Size(min = 6, max = 50, message = "La contraseña debe tener entre 6 y 50 caracteres.")
        String contrasenia
) {}
