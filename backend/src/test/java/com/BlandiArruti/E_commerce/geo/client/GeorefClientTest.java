package com.BlandiArruti.E_commerce.geo.client;

import com.BlandiArruti.E_commerce.exception.ServicioExternoException;
import com.BlandiArruti.E_commerce.geo.client.dto.GeorefLocalidadesResponse.Localidad;
import com.BlandiArruti.E_commerce.geo.config.GeorefProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeorefClientTest {

    private static final String BASE_URL = "https://apis.datos.gob.ar/georef/api";

    private MockRestServiceServer server;
    private GeorefClient client;

    @BeforeEach
    void setUp() {
        GeorefProperties properties = new GeorefProperties();
        properties.setBaseUrl(BASE_URL);

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeorefClient(properties, builder);
    }

    @Test
    void buscarLocalidades_mandaNombreYMaximo_yLeeLaRespuesta() {
        server.expect(requestTo(startsWith(BASE_URL + "/localidades-censales")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("nombre", "mar%20del%20plata"))
                .andExpect(queryParam("max", "10"))
                .andRespond(withSuccess("""
                        {"cantidad":1,"localidades_censales":[
                          {"id":"06357110","nombre":"Mar del Plata",
                           "departamento":{"nombre":"General Pueyrredón"},"provincia":{"nombre":"Buenos Aires"}}
                        ],"total":1}
                        """, MediaType.APPLICATION_JSON));

        List<Localidad> localidades = client.buscarLocalidades("mar del plata", 10);

        assertThat(localidades).hasSize(1);
        assertThat(localidades.getFirst().id()).isEqualTo("06357110");
        assertThat(localidades.getFirst().departamento().nombre()).isEqualTo("General Pueyrredón");
        assertThat(localidades.getFirst().provincia().nombre()).isEqualTo("Buenos Aires");
        server.verify();
    }

    @Test
    void obtenerLocalidad_sinResultados_devuelveVacio() {
        server.expect(requestTo(startsWith(BASE_URL + "/localidades-censales")))
                .andExpect(queryParam("id", "12345678"))
                .andRespond(withSuccess("""
                        {"cantidad":0,"localidades_censales":[],"total":0}
                        """, MediaType.APPLICATION_JSON));

        Optional<Localidad> localidad = client.obtenerLocalidad("12345678");

        assertThat(localidad).isEmpty();
    }

    @Test
    void georefCaido_esServicioExterno() {
        server.expect(requestTo(startsWith(BASE_URL + "/localidades-censales")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.buscarLocalidades("rosario", 10))
                .isInstanceOf(ServicioExternoException.class);
    }
}
