package com.BlandiArruti.E_commerce.dolar.controller;

import com.BlandiArruti.E_commerce.dolar.dto.response.ActualizacionDolarResponse;
import com.BlandiArruti.E_commerce.dolar.dto.response.AjustePreciosResponse;
import com.BlandiArruti.E_commerce.dolar.dto.response.CotizacionDolarResponse;
import com.BlandiArruti.E_commerce.dolar.service.DolarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Dólar", description = "Cotización del dólar y ajuste automático de precios (RF01)")
@RestController
@RequestMapping("/api/v1/dolar")
@RequiredArgsConstructor
public class DolarController {

    private final DolarService dolarService;

    @Operation(summary = "Cotización actual del dólar",
            description = "Pública: la ven el cliente y el admin. Incluye con qué cotización se ajustaron los "
                    + "precios por última vez y cuánto se movió el dólar desde entonces.")
    @GetMapping
    public ResponseEntity<CotizacionDolarResponse> obtenerActual() {
        return ResponseEntity.ok(dolarService.obtenerActual());
    }

    @Operation(summary = "Historial de ajustes de precios (admin)",
            description = "Los últimos 50 ajustes de precios hechos por la variación del dólar.")
    @GetMapping("/historial")
    public ResponseEntity<List<AjustePreciosResponse>> historial() {
        return ResponseEntity.ok(dolarService.historialAjustes());
    }

    @Operation(summary = "Actualizar la cotización ahora (admin)",
            description = "Consulta el dólar en el momento y ajusta los precios si corresponde. Con forzar=true "
                    + "ajusta aunque la variación esté fuera de los márgenes configurados.")
    @PostMapping("/actualizar")
    public ResponseEntity<ActualizacionDolarResponse> actualizar(
            @RequestParam(defaultValue = "false") boolean forzar) {
        return ResponseEntity.ok(dolarService.actualizar(forzar));
    }
}
