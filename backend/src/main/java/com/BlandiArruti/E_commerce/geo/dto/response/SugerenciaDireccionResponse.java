package com.BlandiArruti.E_commerce.geo.dto.response;

/**
 * Una sugerencia del autocompletado de direcciones.
 * principal = "Av. Corrientes 1234", secundario = "CABA, Argentina".
 */
public record SugerenciaDireccionResponse(
        String placeId,
        String principal,
        String secundario
) {}
