package com.BlandiArruti.E_commerce.cotizacion.client;

import com.BlandiArruti.E_commerce.cotizacion.client.dto.CorreoArgentinoRatesRequest;
import com.BlandiArruti.E_commerce.cotizacion.client.dto.CorreoArgentinoRatesResponse;
import com.BlandiArruti.E_commerce.cotizacion.config.CorreoArgentinoProperties;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CorreoArgentinoClientTest {

    private static final String BASE_URL = "https://apitest.correoargentino.com.ar/micorreo/v1";

    private MockRestServiceServer server;
    private CorreoArgentinoClient client;
    private CorreoArgentinoProperties properties;

    @BeforeEach
    void setUp() {
        properties = new CorreoArgentinoProperties();
        properties.setBaseUrl(BASE_URL);
        properties.setUserToken("user-test");
        properties.setPasswordToken("pass-test");
        properties.setCustomerId("CUST123");
        properties.setCodigoPostalOrigen("1425");

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CorreoArgentinoClient(properties, builder);
    }

    @Test
    void cotizar_autenticaYDevuelveTarifasMapeadas() {
        String authBasico = "Basic " + Base64.getEncoder().encodeToString("user-test:pass-test".getBytes());

        server.expect(requestTo(BASE_URL + "/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, authBasico))
                .andRespond(withSuccess("""
                        {"token":"jwt-fake","expiresIn":3600}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(BASE_URL + "/rates"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer jwt-fake"))
                .andRespond(withSuccess("""
                        {"customerId":"CUST123","validTo":"2026-12-31T00:00:00Z","rates":[
                          {"deliveredType":"D","productType":"CP","productName":"Clásico","price":4500.5,"deliveryTimeMin":3,"deliveryTimeMax":5}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        CorreoArgentinoRatesRequest request = new CorreoArgentinoRatesRequest(
                "CUST123", "1425", "7600", "D",
                List.of(new CorreoArgentinoRatesRequest.Dimension(1.0, 20.0, 20.0, 20.0, 1)));

        CorreoArgentinoRatesResponse response = client.cotizar(request);

        assertThat(response.rates()).hasSize(1);
        assertThat(response.rates().get(0).productName()).isEqualTo("Clásico");
        assertThat(response.rates().get(0).price()).isEqualTo(4500.5);
        server.verify();
    }

    @Test
    void cotizar_sinCredencialesConfiguradas_lanzaError() {
        properties.setUserToken("");
        properties.setPasswordToken("");

        CorreoArgentinoRatesRequest request = new CorreoArgentinoRatesRequest(
                "CUST123", "1425", "7600", "D", List.of());

        assertThatThrownBy(() -> client.cotizar(request))
                .isInstanceOf(EcommerceException.class)
                .hasMessageContaining("Correo Argentino");
    }
}
