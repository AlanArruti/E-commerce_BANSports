package com.BlandiArruti.E_commerce.envio.dto.response;

import com.BlandiArruti.E_commerce.cliente.dto.response.DireccionResponse;
import com.BlandiArruti.E_commerce.enums.EstadoEnvio;

import java.time.LocalDate;

public record EnvioResponse(
        Long id,
        String uuidEnvio,
        DireccionResponse direccion,
        EstadoEnvio estado,
        LocalDate fechaSalida,
        LocalDate fechaLlegada
) {}
