package com.BlandiArruti.E_commerce.admin.dto.response;

import java.util.Map;

public record VarianteStockBajoResponse(
        Long idVariante,
        Long idProducto,
        String nombreProducto,
        Map<String, String> atributos,
        Integer stock
) {}
