package com.BlandiArruti.E_commerce.mercadopago.controller;

import com.BlandiArruti.E_commerce.enums.TipoFactura;
import com.BlandiArruti.E_commerce.pedido.service.IPedidoService;
import com.BlandiArruti.E_commerce.mercadopago.service.MercadoPagoService;
import com.mercadopago.resources.payment.Payment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Webhooks", description = "Notificaciones externas (MercadoPago)")
@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final MercadoPagoService mercadoPagoService;
    private final IPedidoService pedidoService;

    @Operation(summary = "Recibir notificación de MercadoPago", description = "Endpoint interno para webhooks IPN de MercadoPago. No requiere autenticación.")
    @PostMapping("/mercadopago")
    public ResponseEntity<Void> recibirNotificacion(@RequestBody Map<String, Object> body) {
        String type = (String) body.get("type");

        if (!"payment".equals(type)) {
            return ResponseEntity.ok().build();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        if (data == null || data.get("id") == null) {
            return ResponseEntity.ok().build();
        }

        Long paymentId = Long.valueOf(data.get("id").toString());
        log.info("Webhook MP recibido: type={}, paymentId={}", type, paymentId);

        try {
            Payment payment = mercadoPagoService.obtenerPago(paymentId);

            if (!"approved".equals(payment.getStatus())) {
                log.info("Pago {} no aprobado (status={}), ignorando.", paymentId, payment.getStatus());
                return ResponseEntity.ok().build();
            }

            String externalReference = payment.getExternalReference();
            if (externalReference == null || !externalReference.contains("|")) {
                log.warn("Pago {} sin external_reference válido: {}", paymentId, externalReference);
                return ResponseEntity.ok().build();
            }

            String[] partes = externalReference.split("\\|");
            if (partes.length < 2) {
                log.warn("Pago {} external_reference malformado: {}", paymentId, externalReference);
                return ResponseEntity.ok().build();
            }

            Long pedidoId = Long.valueOf(partes[0]);
            TipoFactura tipoFactura = TipoFactura.valueOf(partes[1]);

            pedidoService.confirmarPagoMercadoPago(pedidoId, tipoFactura);
            log.info("Pago confirmado para pedido {} via MercadoPago (paymentId={})", pedidoId, paymentId);

        } catch (Exception e) {
            log.error("Error procesando webhook MP (paymentId={}): {}", paymentId, e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}
