package com.BlandiArruti.E_commerce.cotizacion.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CorreoArgentinoRatesRequest(
        String customerId,
        String postalCodeOrigin,
        String postalCodeDestination,
        String deliveryType,
        List<Dimension> dimensions
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Dimension(
            Double weight,
            Double height,
            Double width,
            Double length,
            Integer quantity
    ) {}
}
