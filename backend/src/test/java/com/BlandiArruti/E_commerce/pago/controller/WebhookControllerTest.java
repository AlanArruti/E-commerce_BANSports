package com.BlandiArruti.E_commerce.pago.controller;

import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.exception.ServicioExternoException;
import com.BlandiArruti.E_commerce.pago.service.PagoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    @Mock PagoService pagoService;

    @InjectMocks
    WebhookController webhookController;

    @Test
    void avisoDeUnaPasarela_loPasaAlServicioYResponde200() {
        Map<String, Object> cuerpo = Map.of("type", "order", "data", Map.of("id", "ORD1"));
        Map<String, String> parametros = Map.of("type", "order", "data.id", "ORD1");

        var respuesta = webhookController.recibir("mercadopago", cuerpo, parametros);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(pagoService).procesarNotificacion("mercadopago", cuerpo, parametros);
    }

    @Test
    void pasarelaInexistente_responde404() {
        doThrow(new EntidadNoEncontradaException("No existe la pasarela"))
                .when(pagoService).procesarNotificacion(eq("inventada"), any(), any());

        assertThat(webhookController.recibir("inventada", null, Map.of()).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void errorAlProcesar_igualResponde200ParaQueNoReintente() {
        doThrow(new ServicioExternoException("MercadoPago caído"))
                .when(pagoService).procesarNotificacion(eq("mercadopago"), any(), any());

        assertThat(webhookController.recibir("mercadopago", Map.of(), Map.of()).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }
}
