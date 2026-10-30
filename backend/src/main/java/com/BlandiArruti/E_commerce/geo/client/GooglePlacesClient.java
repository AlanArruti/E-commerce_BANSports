package com.BlandiArruti.E_commerce.geo.client;

import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.exception.ServicioExternoException;
import com.BlandiArruti.E_commerce.geo.client.dto.GoogleAutocompletarRequest;
import com.BlandiArruti.E_commerce.geo.client.dto.GoogleAutocompletarResponse;
import com.BlandiArruti.E_commerce.geo.client.dto.GoogleLugarResponse;
import com.BlandiArruti.E_commerce.geo.config.GoogleMapsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Cliente de Google Places API (New): autocompletado de direcciones y detalle de un lugar.
 * Docs: https://developers.google.com/maps/documentation/places/web-service/op-overview
 *
 * El sessionToken agrupa las sugerencias y el detalle de una misma búsqueda,
 * así Google cobra la búsqueda completa como una sola sesión.
 */
@Component
@RequiredArgsConstructor
public class GooglePlacesClient {

    private static final String IDIOMA = "es";
    private static final String CABECERA_KEY = "X-Goog-Api-Key";
    private static final String CABECERA_CAMPOS = "X-Goog-FieldMask";
    /** Solo se piden los campos que usamos: Google cobra según los campos pedidos. */
    private static final String CAMPOS_LUGAR = "id,formattedAddress,addressComponents";

    private final GoogleMapsProperties properties;
    private final RestClient.Builder restClientBuilder;

    private RestClient restClient;

    private RestClient cliente() {
        if (!properties.estaConfigurada()) {
            throw new ServicioExternoException(
                    "La búsqueda de direcciones no está configurada (falta la API key de Google Maps).");
        }
        if (restClient == null) {
            restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        }
        return restClient;
    }

    public List<GoogleAutocompletarResponse.PlacePrediction> autocompletar(String texto, String sessionToken) {
        GoogleAutocompletarRequest cuerpo = new GoogleAutocompletarRequest(
                texto, List.of(properties.getRegion()), IDIOMA, sessionToken);
        try {
            GoogleAutocompletarResponse respuesta = cliente().post()
                    .uri("/places:autocomplete")
                    .header(CABECERA_KEY, properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(GoogleAutocompletarResponse.class);
            if (respuesta == null || respuesta.suggestions() == null) {
                return List.of();
            }
            return respuesta.suggestions().stream()
                    .map(GoogleAutocompletarResponse.Sugerencia::placePrediction)
                    .filter(p -> p != null && p.placeId() != null)
                    .toList();
        } catch (RestClientException e) {
            throw new ServicioExternoException("No pudimos buscar direcciones en este momento.", e);
        }
    }

    public GoogleLugarResponse obtenerLugar(String placeId, String sessionToken) {
        try {
            return cliente().get()
                    .uri(uri -> {
                        uri.path("/places/{placeId}").queryParam("languageCode", IDIOMA);
                        if (sessionToken != null && !sessionToken.isBlank()) {
                            uri.queryParam("sessionToken", sessionToken);
                        }
                        return uri.build(placeId);
                    })
                    .header(CABECERA_KEY, properties.getApiKey())
                    .header(CABECERA_CAMPOS, CAMPOS_LUGAR)
                    .retrieve()
                    .body(GoogleLugarResponse.class);
        } catch (HttpClientErrorException e) {
            // Google responde 404 (o 400 con un id mal armado) cuando el lugar no existe.
            if (e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new EntidadNoEncontradaException("No encontramos esa dirección. Buscala de nuevo.");
            }
            throw new ServicioExternoException("No pudimos consultar la dirección en este momento.", e);
        } catch (RestClientException e) {
            throw new ServicioExternoException("No pudimos consultar la dirección en este momento.", e);
        }
    }
}
