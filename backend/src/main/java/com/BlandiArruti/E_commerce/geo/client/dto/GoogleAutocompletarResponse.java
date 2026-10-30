package com.BlandiArruti.E_commerce.geo.client.dto;

import java.util.List;

/**
 * Respuesta de POST /places:autocomplete. Solo se mapean los campos que usamos;
 * las sugerencias de búsqueda (queryPrediction) no traen placePrediction y se descartan.
 */
public record GoogleAutocompletarResponse(List<Sugerencia> suggestions) {

    public record Sugerencia(PlacePrediction placePrediction) {}

    public record PlacePrediction(String placeId, Texto text, FormatoEstructurado structuredFormat) {}

    public record FormatoEstructurado(Texto mainText, Texto secondaryText) {}

    public record Texto(String text) {}
}
