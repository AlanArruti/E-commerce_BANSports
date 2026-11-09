package com.BlandiArruti.E_commerce.pago.pasarela.mercadopago.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Respuesta de POST /v1/orders y GET /v1/orders/{id}. Solo los campos que usamos.
 *
 * Estados de la order: created (esperando el pago), processed (cobrada), action_required,
 * failed, expired, canceled, refunded. Cobrada = status "processed" y status_detail "accredited".
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrdenMercadoPago(
        String id,
        String status,
        @JsonProperty("status_detail") String statusDetail,
        @JsonProperty("external_reference") String externalReference,
        @JsonProperty("total_amount") String totalAmount,
        @JsonProperty("total_paid_amount") String totalPaidAmount,
        @JsonProperty("checkout_url") String checkoutUrl
) {

    public boolean estaCobrada() {
        return "processed".equals(status) && "accredited".equals(statusDetail);
    }

    /** Lo pagado según MercadoPago; 0 si no vino o no se puede leer. */
    public BigDecimal montoPagado() {
        try {
            return totalPaidAmount == null ? BigDecimal.ZERO : new BigDecimal(totalPaidAmount);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
