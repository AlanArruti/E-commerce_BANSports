package com.BlandiArruti.E_commerce.dolar.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;

/** Respuesta de GET /v1/dolares/{casa} de DolarApi (solo los campos que usamos). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DolarApiResponse(
        String casa,
        String nombre,
        Double compra,
        Double venta,
        OffsetDateTime fechaActualizacion
) {}
