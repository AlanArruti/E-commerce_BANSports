package com.BlandiArruti.E_commerce.envio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EnvioRequest(
        @NotBlank(message = "La empresa de envío es obligatoria.")
        @Size(max = 60, message = "La empresa de envío puede tener hasta 60 caracteres.")
        String empresa,

        @NotBlank(message = "El número de seguimiento es obligatorio.")
        @Size(max = 60, message = "El número de seguimiento puede tener hasta 60 caracteres.")
        String numeroSeguimiento,

        @NotNull(message = "La fecha de salida es obligatoria.")
        LocalDate fechaSalida,

        @NotNull(message = "La fecha de llegada es obligatoria.")
        LocalDate fechaLlegada
) {}
