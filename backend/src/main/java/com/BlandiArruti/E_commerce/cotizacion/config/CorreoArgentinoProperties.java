package com.BlandiArruti.E_commerce.cotizacion.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "correo-argentino")
public class CorreoArgentinoProperties {

    private String baseUrl;
    private String userToken;
    private String passwordToken;
    private String customerId;
    private String codigoPostalOrigen;
}
