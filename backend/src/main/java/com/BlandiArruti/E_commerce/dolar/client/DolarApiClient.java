package com.BlandiArruti.E_commerce.dolar.client;

import com.BlandiArruti.E_commerce.dolar.client.dto.DolarApiResponse;
import com.BlandiArruti.E_commerce.dolar.config.DolarProperties;
import com.BlandiArruti.E_commerce.exception.ServicioExternoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente de DolarApi (dolarapi.com). Devuelve compra y venta de cada tipo de dólar con
 * la fecha en que se actualizó. Docs: https://dolarapi.com/docs/argentina/
 */
@Component
@RequiredArgsConstructor
public class DolarApiClient {

    private final DolarProperties properties;
    private final RestClient.Builder restClientBuilder;

    private RestClient restClient;

    private RestClient cliente() {
        if (restClient == null) {
            restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        }
        return restClient;
    }

    /** Cotización actual del dólar configurado (dolar.casa). */
    public DolarApiResponse obtenerCotizacion() {
        DolarApiResponse respuesta;
        try {
            respuesta = cliente().get()
                    .uri("/v1/dolares/{casa}", properties.getCasa())
                    .retrieve()
                    .body(DolarApiResponse.class);
        } catch (RestClientException e) {
            throw new ServicioExternoException("No pudimos consultar la cotización del dólar en este momento.", e);
        }
        if (respuesta == null || respuesta.venta() == null || respuesta.venta() <= 0) {
            throw new ServicioExternoException("La API del dólar devolvió una cotización inválida.");
        }
        return respuesta;
    }
}
