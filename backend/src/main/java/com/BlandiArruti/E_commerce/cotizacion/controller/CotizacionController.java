package com.BlandiArruti.E_commerce.cotizacion.controller;

import com.BlandiArruti.E_commerce.cotizacion.dto.response.CotizacionEnvioResponse;
import com.BlandiArruti.E_commerce.cotizacion.service.CotizacionEnvioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Cotización", description = "Cotización de envío por dirección")
@RestController
@RequestMapping("/api/v1/cotizacion")
@RequiredArgsConstructor
public class CotizacionController {

    private final CotizacionEnvioService cotizacionEnvioService;

    @Operation(summary = "Cotizar envío por dirección", description = "Devuelve opciones de envío disponibles para la dirección del cliente.")
    @GetMapping("/envio/{idDireccion}")
    public ResponseEntity<List<CotizacionEnvioResponse>> cotizarEnvio(@PathVariable Long idDireccion) {
        return ResponseEntity.ok(cotizacionEnvioService.cotizarPorDireccion(idDireccion));
    }
}
