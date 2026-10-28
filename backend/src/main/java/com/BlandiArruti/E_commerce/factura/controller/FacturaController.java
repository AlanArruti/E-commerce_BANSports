package com.BlandiArruti.E_commerce.factura.controller;

import com.BlandiArruti.E_commerce.enums.TipoFactura;
import com.BlandiArruti.E_commerce.factura.dto.response.FacturaResponse;
import com.BlandiArruti.E_commerce.factura.service.IFacturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Facturas", description = "Consulta de facturas generadas al pagar")
@RestController
@RequestMapping("/api/v1/factura")
@RequiredArgsConstructor
public class FacturaController {

    private final IFacturaService facturaService;

    @Operation(summary = "Listar facturas", description = "Filtrables por tipo, idPedido o idCliente.")
    @GetMapping
    public ResponseEntity<List<FacturaResponse>> listarTodas(
            @RequestParam(required = false) TipoFactura tipo,
            @RequestParam(required = false) Long idPedido,
            @RequestParam(required = false) Long idCliente) {
        return ResponseEntity.ok(facturaService.listarTodas(tipo, idPedido, idCliente));
    }

    @Operation(summary = "Buscar factura por ID")
    @GetMapping("/{id}")
    public ResponseEntity<FacturaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.buscarPorId(id));
    }
}
