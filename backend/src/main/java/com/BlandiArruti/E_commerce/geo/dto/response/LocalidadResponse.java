package com.BlandiArruti.E_commerce.geo.dto.response;

/** Una localidad de Georef para el buscador del formulario de dirección. */
public record LocalidadResponse(
        /** Id de Georef (8 dígitos): es lo que el front manda al guardar la dirección. */
        String id,
        String nombre,
        /** Departamento o partido, para distinguir localidades con el mismo nombre. */
        String departamento,
        String provincia
) {}
