package com.BlandiArruti.E_commerce.envio.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EnvioRequest(
        @NotNull(message = "La fecha de salida es obligatoria.")
        LocalDate fechaSalida,

        @NotNull(message = "La fecha de llegada es obligatoria.")
        LocalDate fechaLlegada
) {}
