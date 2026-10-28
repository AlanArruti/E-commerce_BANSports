package com.BlandiArruti.E_commerce.geo.dto.response;

public record ProvinciaResponse(
        Long idProvincia,
        String nombreProvincia,
        PaisResponse pais
) {}
