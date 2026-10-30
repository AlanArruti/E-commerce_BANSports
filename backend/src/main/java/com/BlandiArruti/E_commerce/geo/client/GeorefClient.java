package com.BlandiArruti.E_commerce.geo.client;

import com.BlandiArruti.E_commerce.exception.ServicioExternoException;
import com.BlandiArruti.E_commerce.geo.client.dto.GeorefLocalidadesResponse;
import com.BlandiArruti.E_commerce.geo.client.dto.GeorefLocalidadesResponse.Localidad;
import com.BlandiArruti.E_commerce.geo.config.GeorefProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Cliente de Georef (apis.datos.gob.ar/georef). Usamos las localidades censales del INDEC:
 * cada una tiene un id fijo de 8 dígitos, su departamento/partido y su provincia.
 */
@Component
@RequiredArgsConstructor
public class GeorefClient {

    private static final String CAMPOS = "id,nombre,departamento.nombre,provincia.nombre";

    private final GeorefProperties properties;
    private final RestClient.Builder restClientBuilder;

    private RestClient restClient;

    private RestClient cliente() {
        if (restClient == null) {
            restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        }
        return restClient;
    }

    public List<Localidad> buscarLocalidades(String nombre, int maximo) {
        return consultar(uri -> uri.path("/localidades-censales")
                        .queryParam("nombre", nombre)
                        .queryParam("max", maximo)
                        .queryParam("campos", CAMPOS)
                        .build(),
                "No pudimos buscar localidades en este momento.");
    }

    public Optional<Localidad> obtenerLocalidad(String id) {
        return consultar(uri -> uri.path("/localidades-censales")
                        .queryParam("id", id)
                        .queryParam("campos", CAMPOS)
                        .build(),
                "No pudimos consultar la localidad en este momento.")
                .stream()
                .findFirst();
    }

    private List<Localidad> consultar(Function<UriBuilder, URI> uri, String mensajeError) {
        try {
            GeorefLocalidadesResponse respuesta = cliente().get()
                    .uri(uri)
                    .retrieve()
                    .body(GeorefLocalidadesResponse.class);
            if (respuesta == null || respuesta.localidades() == null) {
                return List.of();
            }
            return respuesta.localidades();
        } catch (RestClientException e) {
            throw new ServicioExternoException(mensajeError, e);
        }
    }
}
