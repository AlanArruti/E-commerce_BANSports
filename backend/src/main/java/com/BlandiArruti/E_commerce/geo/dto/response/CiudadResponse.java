package com.BlandiArruti.E_commerce.geo.dto.response;

public record CiudadResponse(
        Long idCiudad,
        String nombreCiudad,
        ProvinciaResponse provincia
) {}
