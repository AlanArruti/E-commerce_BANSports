package com.BlandiArruti.E_commerce.geo.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Respuesta de GET /localidades-censales de Georef (solo los campos que usamos). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeorefLocalidadesResponse(
        @JsonProperty("localidades_censales") List<Localidad> localidades
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Localidad(String id, String nombre, Referencia departamento, Referencia provincia) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Referencia(String id, String nombre) {}
}
