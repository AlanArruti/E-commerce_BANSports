package com.BlandiArruti.E_commerce.cliente.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ClienteResponse(
        Long id,
        String uuidCliente,
        String nombre,
        String apellido,
        String dni,
        String email,
        List<DireccionResponse> direcciones,
        LocalDateTime fechaAlta,
        /** false: la cuenta se creó con Google o GitHub y todavía no tiene contraseña propia. */
        boolean tieneContrasenia
) {}
