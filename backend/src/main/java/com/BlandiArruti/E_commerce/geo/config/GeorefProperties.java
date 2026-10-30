package com.BlandiArruti.E_commerce.geo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración de Georef, la API de datos geográficos del Estado argentino.
 * Es gratis y no pide key: solo hace falta la URL.
 * Docs: https://www.argentina.gob.ar/datos-abiertos/georef
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "georef")
public class GeorefProperties {

    private String baseUrl;
}
