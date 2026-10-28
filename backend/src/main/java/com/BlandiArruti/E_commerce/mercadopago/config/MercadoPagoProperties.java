package com.BlandiArruti.E_commerce.mercadopago.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class MercadoPagoProperties {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${mercadopago.notification-url:}")
    private String notificationUrl;

    @Value("${mercadopago.back-url:http://localhost:8080}")
    private String backUrl;

    private static final String BACK_URL_POR_DEFECTO = "http://localhost:8080";

    @PostConstruct
    public void init() {
        // Docker Compose pasa la variable vacía cuando no hay túnel de ngrok
        // configurado, y "" no dispara el default de @Value (que solo aplica si
        // la propiedad no existe). Sin esto las back_urls de la preferencia
        // saldrían malformadas.
        if (backUrl == null || backUrl.isBlank()) {
            backUrl = BACK_URL_POR_DEFECTO;
        }
        com.mercadopago.MercadoPagoConfig.setAccessToken(accessToken);
    }
}
