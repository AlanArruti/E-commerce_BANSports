package com.BlandiArruti.E_commerce.pago.controller;

import com.BlandiArruti.E_commerce.pago.dto.request.IniciarPagoRequest;
import com.BlandiArruti.E_commerce.pago.dto.response.InicioPagoResponse;
import com.BlandiArruti.E_commerce.pago.dto.response.PagoResponse;
import com.BlandiArruti.E_commerce.pago.dto.response.PasarelaResponse;
import com.BlandiArruti.E_commerce.pago.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Pagos", description = "Pago de pedidos con pasarelas (MercadoPago y las que se sumen)")
@RestController
@RequestMapping("/api/v1/pago")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    @Operation(summary = "Pasarelas disponibles",
               description = "Las que tienen credenciales cargadas. El front muestra una opción por cada una.")
    @GetMapping("/pasarelas")
    public ResponseEntity<List<PasarelaResponse>> pasarelas() {
        return ResponseEntity.ok(pagoService.pasarelasDisponibles());
    }

    @Operation(summary = "Iniciar el pago de un pedido",
               description = "Registra el intento y devuelve la URL de la pasarela. Al terminar, la pasarela "
                           + "devuelve al cliente a /pago/resultado?pago={pagoId} del front.")
    @PostMapping("/pedido/{pedidoId}")
    public ResponseEntity<InicioPagoResponse> iniciar(@PathVariable Long pedidoId,
                                                      @Valid @RequestBody IniciarPagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoService.iniciar(pedidoId, request));
    }

    @Operation(summary = "Intentos de pago de un pedido", description = "Del más nuevo al más viejo.")
    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<List<PagoResponse>> historial(@PathVariable Long pedidoId) {
        return ResponseEntity.ok(pagoService.historial(pedidoId));
    }

    @Operation(summary = "Confirmar un pago al volver de la pasarela",
               description = "Consulta el estado real en la pasarela. Si está aprobado y cubre el total, "
                           + "el pedido pasa a PAGADO. Se puede llamar varias veces sin cobrar dos veces.")
    @PostMapping("/{pagoId}/confirmar")
    public ResponseEntity<PagoResponse> confirmar(@PathVariable Long pagoId) {
        return ResponseEntity.ok(pagoService.confirmar(pagoId));
    }
}
