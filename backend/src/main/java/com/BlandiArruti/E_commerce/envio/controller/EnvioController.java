package com.BlandiArruti.E_commerce.envio.controller;

import com.BlandiArruti.E_commerce.envio.dto.request.EnvioRequest;
import com.BlandiArruti.E_commerce.envio.dto.request.EstadoEnvioRequest;
import com.BlandiArruti.E_commerce.envio.dto.response.EnvioResponse;
import com.BlandiArruti.E_commerce.envio.service.IEnvioService;
import com.BlandiArruti.E_commerce.enums.EstadoEnvio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Envíos", description = "Gestión de envíos (solo ADMIN)")
@RestController
@RequestMapping("/api/v1/envio")
@RequiredArgsConstructor
public class EnvioController {

    private final IEnvioService envioService;

    @Operation(summary = "Listar envíos", description = "Filtrable por estado.")
    @GetMapping
    public ResponseEntity<List<EnvioResponse>> listarTodos(
            @RequestParam(required = false) EstadoEnvio estado) {
        return ResponseEntity.ok(envioService.listarTodos(estado));
    }

    @Operation(summary = "Buscar envío por ID")
    @GetMapping("/{id}")
    public ResponseEntity<EnvioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(envioService.buscarPorId(id));
    }

    @Operation(summary = "Crear envío para un pedido")
    @PostMapping("/pedido/{idPedido}")
    public ResponseEntity<EnvioResponse> crear(@PathVariable Long idPedido,
                                               @Valid @RequestBody EnvioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(envioService.crear(idPedido, request));
    }

    @Operation(summary = "Actualizar estado del envío", description = "Transiciones: DESPACHADO → EN_CAMINO → ENTREGADO")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<EnvioResponse> actualizarEstado(@PathVariable Long id,
                                                          @Valid @RequestBody EstadoEnvioRequest request) {
        return ResponseEntity.ok(envioService.actualizarEstado(id, request));
    }

    @Operation(summary = "Eliminar envío")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        envioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
