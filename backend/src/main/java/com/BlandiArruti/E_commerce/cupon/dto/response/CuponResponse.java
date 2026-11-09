package com.BlandiArruti.E_commerce.cupon.dto.response;

import com.BlandiArruti.E_commerce.enums.TipoCupon;

import java.time.LocalDate;

/** Cupón para el panel, con su estado calculado a hoy. */
public record CuponResponse(
        Long id,
        String codigo,
        TipoCupon tipo,
        Double valor,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        Double montoMinimo,
        Integer usosMaximos,
        int usos,
        boolean activo,
        Estado estado
) {

    /** Cómo está hoy: es lo que se muestra en la columna "Estado" del panel. */
    public enum Estado { VIGENTE, PROGRAMADO, VENCIDO, AGOTADO, INACTIVO }
}
