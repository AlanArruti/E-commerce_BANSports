package com.BlandiArruti.E_commerce.auth.dto.response;

public record TokenResponse(
        String token,
        String tipo,
        String rol,
        Long id
) {}
