package com.BlandiArruti.E_commerce.cotizacion.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CorreoArgentinoTokenResponse(
        String token,
        Long expiresIn
) {}
