package com.BlandiArruti.E_commerce.geo.client;

import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.exception.ServicioExternoException;
import com.BlandiArruti.E_commerce.geo.client.dto.GoogleAutocompletarResponse;
import com.BlandiArruti.E_commerce.geo.client.dto.GoogleLugarResponse;
import com.BlandiArruti.E_commerce.geo.config.GoogleMapsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GooglePlacesClientTest {

    private static final String BASE_URL = "https://places.googleapis.com/v1";

    private MockRestServiceServer server;
    private GooglePlacesClient client;
    private GoogleMapsProperties properties;

    @BeforeEach
    void setUp() {
        properties = new GoogleMapsProperties();
        properties.setBaseUrl(BASE_URL);
        properties.setApiKey("key-test");
        properties.setRegion("AR");

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GooglePlacesClient(properties, builder);
    }

    @Test
    void autocompletar_mandaKeyRegionYSesion_yDescartaSugerenciasSinLugar() {
        server.expect(requestTo(BASE_URL + "/places:autocomplete"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Goog-Api-Key", "key-test"))
                .andExpect(content().json("""
                        {"input":"corrientes 1234","includedRegionCodes":["AR"],"languageCode":"es","sessionToken":"ses-1"}
                        """))
                .andRespond(withSuccess("""
                        {"suggestions":[
                          {"placePrediction":{"placeId":"ChIJabc","text":{"text":"Av. Corrientes 1234, CABA, Argentina"},
                            "structuredFormat":{"mainText":{"text":"Av. Corrientes 1234"},"secondaryText":{"text":"CABA, Argentina"}}}},
                          {"queryPrediction":{"text":{"text":"corrientes 1234 pizzerías"}}}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        List<GoogleAutocompletarResponse.PlacePrediction> sugerencias = client.autocompletar("corrientes 1234", "ses-1");

        assertThat(sugerencias).hasSize(1);
        assertThat(sugerencias.get(0).placeId()).isEqualTo("ChIJabc");
        assertThat(sugerencias.get(0).structuredFormat().mainText().text()).isEqualTo("Av. Corrientes 1234");
        server.verify();
    }

    @Test
    void obtenerLugar_pideSoloLosCamposQueUsamos() {
        server.expect(requestTo(BASE_URL + "/places/ChIJabc?languageCode=es&sessionToken=ses-1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Goog-Api-Key", "key-test"))
                .andExpect(header("X-Goog-FieldMask", "id,formattedAddress,addressComponents"))
                .andRespond(withSuccess("""
                        {"id":"ChIJabc","formattedAddress":"Av. Corrientes 1234, C1043 CABA, Argentina",
                         "addressComponents":[
                           {"longText":"1234","shortText":"1234","types":["street_number"]},
                           {"longText":"Avenida Corrientes","shortText":"Av. Corrientes","types":["route"]}
                         ]}
                        """, MediaType.APPLICATION_JSON));

        GoogleLugarResponse lugar = client.obtenerLugar("ChIJabc", "ses-1");

        assertThat(lugar.componente("route")).get()
                .extracting(GoogleLugarResponse.Componente::longText).isEqualTo("Avenida Corrientes");
        server.verify();
    }

    @Test
    void obtenerLugar_inexistente_lanzaNoEncontrado() {
        server.expect(requestTo(BASE_URL + "/places/ChIJnoexiste?languageCode=es"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.obtenerLugar("ChIJnoexiste", null))
                .isInstanceOf(EntidadNoEncontradaException.class);
    }

    @Test
    void googleCaido_lanzaServicioExterno() {
        server.expect(requestTo(BASE_URL + "/places:autocomplete")).andRespond(withServerError());

        assertThatThrownBy(() -> client.autocompletar("corrientes", null))
                .isInstanceOf(ServicioExternoException.class);
    }

    @Test
    void sinApiKey_lanzaServicioExternoSinLlamarAGoogle() {
        properties.setApiKey("");

        assertThatThrownBy(() -> client.autocompletar("corrientes", null))
                .isInstanceOf(ServicioExternoException.class)
                .hasMessageContaining("API key");
        server.verify();
    }
}
