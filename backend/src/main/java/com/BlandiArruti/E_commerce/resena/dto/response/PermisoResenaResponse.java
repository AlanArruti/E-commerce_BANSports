package com.BlandiArruti.E_commerce.resena.dto.response;

/**
 * Si quien consulta puede opinar sobre el producto. El front lo usa para decidir
 * si muestra el formulario, la reseña propia o un aviso.
 */
public record PermisoResenaResponse(
        boolean puedeOpinar,
        /** OK, SIN_SESION, NO_ES_CLIENTE, NO_LO_COMPRO o YA_OPINO. */
        String motivo,
        /** La reseña que ya dejó, si motivo es YA_OPINO. */
        ResenaResponse resenaPropia
) {}
