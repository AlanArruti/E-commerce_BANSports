package com.BlandiArruti.E_commerce.pago.pasarela.mercadopago.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Body de POST /v1/orders para Checkout Pro.
 * Los montos van como texto con dos decimales ("1500.00"), como pide la API.
 * La suma de unit_price × quantity de los ítems tiene que dar exacto total_amount.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrdenRequest(
        String type,
        @JsonProperty("processing_mode") String processingMode,
        @JsonProperty("capture_mode") String captureMode,
        @JsonProperty("total_amount") String totalAmount,
        @JsonProperty("external_reference") String externalReference,
        String description,
        List<Item> items,
        Config config
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Item(
            String title,
            Integer quantity,
            @JsonProperty("unit_price") String unitPrice,
            @JsonProperty("external_code") String externalCode
    ) {}

    public record Config(Online online) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Online(
            @JsonProperty("success_url") String successUrl,
            @JsonProperty("failure_url") String failureUrl,
            @JsonProperty("pending_url") String pendingUrl,
            @JsonProperty("auto_return") String autoReturn
    ) {}
}
