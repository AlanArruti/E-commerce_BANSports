package com.BlandiArruti.E_commerce.cotizacion.client;

import com.BlandiArruti.E_commerce.cotizacion.client.dto.CorreoArgentinoRatesRequest;
import com.BlandiArruti.E_commerce.cotizacion.client.dto.CorreoArgentinoRatesResponse;
import com.BlandiArruti.E_commerce.cotizacion.client.dto.CorreoArgentinoTokenResponse;
import com.BlandiArruti.E_commerce.cotizacion.config.CorreoArgentinoProperties;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class CorreoArgentinoClient {

    private static final long MARGEN_EXPIRACION_SEGUNDOS = 60;

    private final CorreoArgentinoProperties properties;
    private final RestClient.Builder restClientBuilder;

    private RestClient restClient;
    private String token;
    private Instant tokenExpiracion;

    private RestClient cliente() {
        if (restClient == null) {
            restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        }
        return restClient;
    }

    private synchronized String obtenerToken() {
        if (token != null && tokenExpiracion != null && Instant.now().isBefore(tokenExpiracion)) {
            return token;
        }

        if (properties.getUserToken() == null || properties.getUserToken().isBlank()
                || properties.getPasswordToken() == null || properties.getPasswordToken().isBlank()) {
            throw new EcommerceException(
                    "La cotización de envíos no está configurada (faltan credenciales de Correo Argentino).");
        }

        try {
            CorreoArgentinoTokenResponse response = cliente().post()
                    .uri("/token")
                    .headers(headers -> headers.setBasicAuth(properties.getUserToken(), properties.getPasswordToken()))
                    .retrieve()
                    .body(CorreoArgentinoTokenResponse.class);

            if (response == null || response.token() == null || response.token().isBlank()) {
                throw new EcommerceException("Correo Argentino no devolvió un token válido.");
            }

            long ttlSegundos = response.expiresIn() != null ? response.expiresIn() : 3600;
            token = response.token();
            tokenExpiracion = Instant.now().plusSeconds(Math.max(ttlSegundos - MARGEN_EXPIRACION_SEGUNDOS, 0));
            return token;
        } catch (RestClientException e) {
            throw new EcommerceException("No se pudo autenticar con Correo Argentino.", e);
        }
    }

    public CorreoArgentinoRatesResponse cotizar(CorreoArgentinoRatesRequest request) {
        String jwt = obtenerToken();

        try {
            return cliente().post()
                    .uri("/rates")
                    .headers(headers -> headers.setBearerAuth(jwt))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(CorreoArgentinoRatesResponse.class);
        } catch (RestClientException e) {
            throw new EcommerceException("No se pudo obtener la cotización de Correo Argentino.", e);
        }
    }
}
