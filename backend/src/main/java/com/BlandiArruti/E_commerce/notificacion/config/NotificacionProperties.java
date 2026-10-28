package com.BlandiArruti.E_commerce.notificacion.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "notificaciones")
public class NotificacionProperties {

    private String remitente;
}
