package com.BlandiArruti.E_commerce.cotizacion.controller;

import com.BlandiArruti.E_commerce.cotizacion.dto.request.TarifaEnvioRequest;
import com.BlandiArruti.E_commerce.cotizacion.dto.response.TarifaEnvioResponse;
import com.BlandiArruti.E_commerce.cotizacion.service.ITarifaEnvioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Tarifas de envío", description = "Tabla propia de tarifas por provincia (respaldo de la cotización externa). Solo admin.")
@RestController
@RequestMapping("/api/v1/cotizacion/tarifas")
@RequiredArgsConstructor
public class TarifaEnvioController {

    private final ITarifaEnvioService tarifaEnvioService;

    @Operation(summary = "Listar tarifas de envío")
    @GetMapping
    public ResponseEntity<List<TarifaEnvioResponse>> listar() {
        return ResponseEntity.ok(tarifaEnvioService.listarTodas());
    }

    @Operation(summary = "Buscar tarifa de envío por ID")
    @GetMapping("/{id}")
    public ResponseEntity<TarifaEnvioResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(tarifaEnvioService.buscarPorId(id));
    }

    @Operation(summary = "Crear tarifa de envío", description = "Con provincia nula se crea/actualiza la tarifa por defecto.")
    @PostMapping
    public ResponseEntity<TarifaEnvioResponse> crear(@Valid @RequestBody TarifaEnvioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tarifaEnvioService.crear(request));
    }

    @Operation(summary = "Actualizar tarifa de envío")
    @PutMapping("/{id}")
    public ResponseEntity<TarifaEnvioResponse> actualizar(@PathVariable Long id,
                                                          @Valid @RequestBody TarifaEnvioRequest request) {
        return ResponseEntity.ok(tarifaEnvioService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar tarifa de envío")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        tarifaEnvioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
