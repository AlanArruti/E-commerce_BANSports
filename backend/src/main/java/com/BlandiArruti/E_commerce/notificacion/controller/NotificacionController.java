package com.BlandiArruti.E_commerce.notificacion.controller;

import com.BlandiArruti.E_commerce.notificacion.dto.response.NoLeidasResponse;
import com.BlandiArruti.E_commerce.notificacion.dto.response.NotificacionResponse;
import com.BlandiArruti.E_commerce.notificacion.service.NotificacionClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Notificaciones", description = "Avisos del cliente en la página: estado de sus pedidos y ofertas (RF02)")
@RestController
@RequestMapping("/api/v1/cliente/{idCliente}/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionClienteService notificacionClienteService;

    @Operation(summary = "Listar notificaciones", description = "Las últimas 30, de la más nueva a la más vieja.")
    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> listar(@PathVariable Long idCliente) {
        return ResponseEntity.ok(notificacionClienteService.listar(idCliente));
    }

    @Operation(summary = "Cantidad de no leídas", description = "Para el contador de la campanita.")
    @GetMapping("/no-leidas")
    public ResponseEntity<NoLeidasResponse> contarNoLeidas(@PathVariable Long idCliente) {
        return ResponseEntity.ok(notificacionClienteService.contarNoLeidas(idCliente));
    }

    @Operation(summary = "Marcar una notificación como leída")
    @PatchMapping("/{idNotificacion}/leida")
    public ResponseEntity<NotificacionResponse> marcarLeida(@PathVariable Long idCliente,
                                                            @PathVariable Long idNotificacion) {
        return ResponseEntity.ok(notificacionClienteService.marcarLeida(idCliente, idNotificacion));
    }

    @Operation(summary = "Marcar todas como leídas")
    @PatchMapping("/leidas")
    public ResponseEntity<NoLeidasResponse> marcarTodasLeidas(@PathVariable Long idCliente) {
        return ResponseEntity.ok(notificacionClienteService.marcarTodasLeidas(idCliente));
    }
}
