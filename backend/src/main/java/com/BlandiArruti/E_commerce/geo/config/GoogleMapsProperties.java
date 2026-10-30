package com.BlandiArruti.E_commerce.geo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración de Google Maps (Places API "New").
 * La key va solo en el back (variable GOOGLE_MAPS_API_KEY): el repo es público
 * y el front le pide las sugerencias a nuestra API, nunca a Google directo.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "google-maps")
public class GoogleMapsProperties {

    private String baseUrl;
    private String apiKey;
    /** Código ISO del país al que se limitan las búsquedas (hoy solo se envía dentro de Argentina). */
    private String region;

    public boolean estaConfigurada() {
        return apiKey != null && !apiKey.isBlank();
    }
}
