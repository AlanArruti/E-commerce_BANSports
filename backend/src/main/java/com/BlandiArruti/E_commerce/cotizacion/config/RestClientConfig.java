package com.BlandiArruti.E_commerce.cotizacion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder().requestFactory(conTimeouts(Duration.ofSeconds(10)));
    }

    /**
     * Sin timeout, una API externa colgada (Georef, Correo Argentino, MercadoPago, GitHub) deja el
     * request del cliente esperando para siempre. Con esto falla rápido y el error llega al usuario.
     */
    public static ClientHttpRequestFactory conTimeouts(Duration lectura) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(lectura);
        return factory;
    }
}
