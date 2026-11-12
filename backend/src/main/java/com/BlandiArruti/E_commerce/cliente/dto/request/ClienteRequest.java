package com.BlandiArruti.E_commerce.cliente.dto.request;

import jakarta.validation.constraints.*;

public record ClienteRequest(
        @NotBlank(message = "El nombre no puede estar vacío.")
        @Size(max = 50, message = "El nombre no puede superar los 50 caracteres.")
        @Pattern(regexp = FormatosCliente.SOLO_LETRAS, message = "El nombre solo puede tener letras y espacios.")
        String nombre,

        @NotBlank(message = "El apellido no puede estar vacío.")
        @Size(max = 50, message = "El apellido no puede superar los 50 caracteres.")
        @Pattern(regexp = FormatosCliente.SOLO_LETRAS, message = "El apellido solo puede tener letras y espacios.")
        String apellido,

        @NotBlank(message = "El DNI no puede estar vacío.")
        @Pattern(regexp = FormatosCliente.DNI, message = "El DNI debe tener 7 u 8 números, sin puntos ni espacios.")
        String dni,

        @NotBlank(message = "El email no puede estar vacío.")
        @Pattern(regexp = FormatosCliente.EMAIL, message = FormatosCliente.MENSAJE_EMAIL)
        @Size(max = 100, message = "El email no puede superar los 100 caracteres.")
        String email,

        @NotBlank(message = "La contraseña no puede estar vacía.")
        @Size(min = 6, max = 50, message = "La contraseña debe tener entre 6 y 50 caracteres.")
        String contrasenia
) {}
