package com.BlandiArruti.E_commerce.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecuperarPasswordRequest(
        @NotBlank(message = "El email no puede estar vacío.")
        @Email(message = "El email no tiene formato válido.")
        @Size(max = 100, message = "El email no puede superar los 100 caracteres.")
        String email
) {}
