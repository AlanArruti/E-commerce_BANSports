package com.BlandiArruti.E_commerce.envio.dto.response;

import com.BlandiArruti.E_commerce.cliente.dto.response.DireccionResponse;
import com.BlandiArruti.E_commerce.enums.EstadoEnvio;

import java.time.LocalDate;

public record EnvioResponse(
        Long id,
        String uuidEnvio,
        /** Para el listado de envíos del panel. */
        Long idPedido,
        /** "Juan Pérez". */
        String cliente,
        DireccionResponse direccion,
        EstadoEnvio estado,
        LocalDate fechaSalida,
        LocalDate fechaLlegada,
        String empresa,
        String numeroSeguimiento
) {}
