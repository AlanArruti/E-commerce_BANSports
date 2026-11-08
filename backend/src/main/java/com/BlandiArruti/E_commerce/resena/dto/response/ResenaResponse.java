package com.BlandiArruti.E_commerce.resena.dto.response;

import java.time.LocalDateTime;

public record ResenaResponse(
        Long id,
        /** Nombre e inicial del apellido ("María G."): no se expone el nombre completo. */
        String autor,
        Integer estrellas,
        String comentario,
        LocalDateTime fecha
) {}
