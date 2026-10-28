package com.BlandiArruti.E_commerce.producto.dto.response;

import java.util.Map;

public record VarianteResponse(
        Long id,
        String uuidVariante,
        Map<String, String> atributos,
        Integer stock,
        boolean activo,
        Long idProducto
) {}
