package com.BlandiArruti.E_commerce.auth.dto.response;

/**
 * Identidad del usuario detrás del token. Le permite al front saber quién está
 * logueado sin tener que decodificar el JWT por su cuenta.
 */
public record UsuarioActualResponse(
        Long id,
        String username,
        String rol
) {}
