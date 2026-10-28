package com.BlandiArruti.E_commerce.cliente.dto.response;

import java.util.List;

public record ClienteResponse(
        Long id,
        String uuidCliente,
        String nombre,
        String apellido,
        String dni,
        String email,
        List<DireccionResponse> direcciones
) {}
