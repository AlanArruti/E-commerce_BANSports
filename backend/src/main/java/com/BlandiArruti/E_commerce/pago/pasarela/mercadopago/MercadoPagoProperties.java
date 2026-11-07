package com.BlandiArruti.E_commerce.pago.pasarela.mercadopago;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Credencial de MercadoPago (MP_ACCESS_TOKEN en el .env). Sin ella la pasarela queda
 * no disponible y el front no ofrece pagar con MercadoPago.
 */
@Component
@Getter
public class MercadoPagoProperties {

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    public boolean estaConfigurado() {
        return accessToken != null && !accessToken.isBlank();
    }
}
