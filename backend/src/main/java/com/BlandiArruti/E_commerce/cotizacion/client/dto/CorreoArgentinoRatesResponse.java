package com.BlandiArruti.E_commerce.cotizacion.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CorreoArgentinoRatesResponse(
        String customerId,
        String validTo,
        List<Rate> rates
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Rate(
            String deliveredType,
            String productType,
            String productName,
            Double price,
            Integer deliveryTimeMin,
            Integer deliveryTimeMax
    ) {}
}
