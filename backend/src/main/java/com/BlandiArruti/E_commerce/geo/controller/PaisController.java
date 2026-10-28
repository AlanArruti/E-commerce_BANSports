package com.BlandiArruti.E_commerce.geo.controller;

import com.BlandiArruti.E_commerce.geo.dto.response.PaisResponse;
import com.BlandiArruti.E_commerce.geo.dto.response.ProvinciaResponse;
import com.BlandiArruti.E_commerce.geo.service.GeoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Geografía", description = "Consulta de países, provincias y ciudades")
@RestController
@RequestMapping("/api/v1/paises")
@RequiredArgsConstructor
public class PaisController {

    private final GeoService geoService;

    @Operation(summary = "Listar países")
    @GetMapping
    public ResponseEntity<List<PaisResponse>> listarTodos() {
        return ResponseEntity.ok(geoService.listarPaises());
    }

    @Operation(summary = "Listar provincias de un país")
    @GetMapping("/{id}/provincias")
    public ResponseEntity<List<ProvinciaResponse>> listarProvincias(@PathVariable Long id) {
        return ResponseEntity.ok(geoService.listarProvinciasDePais(id));
    }
}
