package com.BlandiArruti.E_commerce.dolar.client;

import com.BlandiArruti.E_commerce.dolar.client.dto.DolarApiResponse;
import com.BlandiArruti.E_commerce.dolar.config.DolarProperties;
import com.BlandiArruti.E_commerce.exception.ServicioExternoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DolarApiClientTest {

    private static final String BASE_URL = "https://dolarapi.com";

    private MockRestServiceServer server;
    private DolarApiClient client;

    @BeforeEach
    void setUp() {
        DolarProperties properties = new DolarProperties();
        properties.setBaseUrl(BASE_URL);
        properties.setCasa("oficial");

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DolarApiClient(properties, builder);
    }

    @Test
    void obtenerCotizacion_pideElDolarConfigurado_yLeeLaRespuesta() {
        server.expect(requestTo(BASE_URL + "/v1/dolares/oficial"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"moneda":"USD","casa":"oficial","nombre":"Oficial",
                         "compra":1410.5,"venta":1460.5,"fechaActualizacion":"2026-11-24T14:02:00.000Z"}
                        """, MediaType.APPLICATION_JSON));

        DolarApiResponse respuesta = client.obtenerCotizacion();

        assertThat(respuesta.casa()).isEqualTo("oficial");
        assertThat(respuesta.compra()).isEqualTo(1410.5);
        assertThat(respuesta.venta()).isEqualTo(1460.5);
        assertThat(respuesta.fechaActualizacion()).isNotNull();
        server.verify();
    }

    @Test
    void ventaInvalida_esServicioExterno() {
        server.expect(requestTo(BASE_URL + "/v1/dolares/oficial"))
                .andRespond(withSuccess("""
                        {"casa":"oficial","compra":0,"venta":0}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.obtenerCotizacion())
                .isInstanceOf(ServicioExternoException.class);
    }

    @Test
    void apiCaida_esServicioExterno() {
        server.expect(requestTo(BASE_URL + "/v1/dolares/oficial"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.obtenerCotizacion())
                .isInstanceOf(ServicioExternoException.class);
    }
}
