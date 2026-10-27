package com.BlandiArruti.E_commerce.cliente.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos editables del perfil. No incluye la contraseña: para cambiarla está
 * PATCH /api/v1/cliente/{id}/password, que exige la contraseña actual.
 */
public record ClienteUpdateRequest(
        @NotBlank(message = "El nombre no puede estar vacío.")
        @Size(max = 50, message = "El nombre no puede superar los 50 caracteres.")
        String nombre,

        @NotBlank(message = "El apellido no puede estar vacío.")
        @Size(max = 50, message = "El apellido no puede superar los 50 caracteres.")
        String apellido,

        @NotBlank(message = "El email no puede estar vacío.")
        @Email(message = "El email no tiene formato válido.")
        @Size(max = 100, message = "El email no puede superar los 100 caracteres.")
        String email
) {}
