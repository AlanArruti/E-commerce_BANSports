package com.BlandiArruti.E_commerce.pago.controller;

import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.pago.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Un solo webhook para todas las pasarelas: POST /api/v1/webhook/{pasarela}
 * (ej. /api/v1/webhook/mercadopago). Cada pasarela sabe leer su propio formato de aviso.
 *
 * Los datos del aviso no se usan como prueba de pago: el servicio vuelve a consultar el cobro
 * en la API de la pasarela. Por eso un aviso falso no puede marcar un pedido como pagado.
 */
@Tag(name = "Webhooks", description = "Avisos de las pasarelas de pago")
@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final PagoService pagoService;

    @Operation(summary = "Recibir el aviso de una pasarela", description = "Público. Siempre responde 200 a una pasarela conocida.")
    @PostMapping("/{pasarela}")
    public ResponseEntity<Void> recibir(@PathVariable String pasarela,
                                        @RequestBody(required = false) Map<String, Object> cuerpo,
                                        @RequestParam Map<String, String> parametros) {
        try {
            pagoService.procesarNotificacion(pasarela, cuerpo, parametros);
        } catch (EntidadNoEncontradaException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            // Si respondemos error, la pasarela reintenta el mismo aviso durante horas.
            log.error("Error procesando aviso de {}: {}", pasarela, e.getMessage());
        }
        return ResponseEntity.ok().build();
    }
}
