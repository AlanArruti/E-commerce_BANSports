package com.BlandiArruti.E_commerce.geo.client.dto;

import java.util.List;

/** Cuerpo de POST /places:autocomplete (Places API New). */
public record GoogleAutocompletarRequest(
        String input,
        List<String> includedRegionCodes,
        String languageCode,
        String sessionToken
) {}
