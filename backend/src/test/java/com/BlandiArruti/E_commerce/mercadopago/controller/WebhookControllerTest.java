package com.BlandiArruti.E_commerce.mercadopago.controller;

import com.BlandiArruti.E_commerce.enums.TipoFactura;
import com.BlandiArruti.E_commerce.mercadopago.service.MercadoPagoService;
import com.BlandiArruti.E_commerce.pedido.service.IPedidoService;
import com.mercadopago.resources.payment.Payment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    @Mock MercadoPagoService mercadoPagoService;
    @Mock IPedidoService pedidoService;

    @InjectMocks
    WebhookController webhookController;

    // ─── type distinto de "payment" ──────────────────────────────────────────

    @Test
    void recibirNotificacion_typeNoEsPayment_retorna200SinProcesar() {
        Map<String, Object> body = Map.of("type", "merchant_order");

        ResponseEntity<Void> response = webhookController.recibirNotificacion(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verifyNoInteractions(mercadoPagoService, pedidoService);
    }

    @Test
    void recibirNotificacion_sinCampoType_retorna200SinProcesar() {
        Map<String, Object> body = Map.of("action", "payment.updated");

        ResponseEntity<Void> response = webhookController.recibirNotificacion(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verifyNoInteractions(mercadoPagoService, pedidoService);
    }

    // ─── type "payment" sin data válida ──────────────────────────────────────

    @Test
    void recibirNotificacion_typePaymentSinData_retorna200SinProcesar() {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "payment");
        body.put("data", null);

        ResponseEntity<Void> response = webhookController.recibirNotificacion(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verifyNoInteractions(mercadoPagoService, pedidoService);
    }

    // ─── pago aprobado con external_reference válido ─────────────────────────

    @Test
    void recibirNotificacion_pagoAprobado_confirmaYRetorna200() throws Exception {
        Payment payment = mock(Payment.class);
        when(payment.getStatus()).thenReturn("approved");
        when(payment.getExternalReference()).thenReturn("42|B");
        when(mercadoPagoService.obtenerPago(123L)).thenReturn(payment);

        Map<String, Object> data = Map.of("id", "123");
        Map<String, Object> body = Map.of("type", "payment", "data", data);

        ResponseEntity<Void> response = webhookController.recibirNotificacion(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(pedidoService).confirmarPagoMercadoPago(42L, TipoFactura.B);
    }

    // ─── pago no aprobado ────────────────────────────────────────────────────

    @Test
    void recibirNotificacion_pagoRechazado_noConfirmaPedido() throws Exception {
        Payment payment = mock(Payment.class);
        when(payment.getStatus()).thenReturn("rejected");
        when(mercadoPagoService.obtenerPago(456L)).thenReturn(payment);

        Map<String, Object> data = Map.of("id", "456");
        Map<String, Object> body = Map.of("type", "payment", "data", data);

        ResponseEntity<Void> response = webhookController.recibirNotificacion(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(pedidoService, never()).confirmarPagoMercadoPago(anyLong(), any());
    }

    // ─── external_reference malformado ───────────────────────────────────────

    @Test
    void recibirNotificacion_externalReferenceSinSeparador_noConfirmaPedido() throws Exception {
        Payment payment = mock(Payment.class);
        when(payment.getStatus()).thenReturn("approved");
        when(payment.getExternalReference()).thenReturn("referencia-invalida");
        when(mercadoPagoService.obtenerPago(789L)).thenReturn(payment);

        Map<String, Object> data = Map.of("id", "789");
        Map<String, Object> body = Map.of("type", "payment", "data", data);

        ResponseEntity<Void> response = webhookController.recibirNotificacion(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(pedidoService, never()).confirmarPagoMercadoPago(anyLong(), any());
    }

    @Test
    void recibirNotificacion_externalReferenceNulo_noConfirmaPedido() throws Exception {
        Payment payment = mock(Payment.class);
        when(payment.getStatus()).thenReturn("approved");
        when(payment.getExternalReference()).thenReturn(null);
        when(mercadoPagoService.obtenerPago(101L)).thenReturn(payment);

        Map<String, Object> data = Map.of("id", "101");
        Map<String, Object> body = Map.of("type", "payment", "data", data);

        ResponseEntity<Void> response = webhookController.recibirNotificacion(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(pedidoService, never()).confirmarPagoMercadoPago(anyLong(), any());
    }
}
