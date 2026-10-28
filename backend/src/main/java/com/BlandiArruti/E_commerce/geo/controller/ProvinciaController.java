package com.BlandiArruti.E_commerce.geo.controller;

import com.BlandiArruti.E_commerce.geo.dto.response.CiudadResponse;
import com.BlandiArruti.E_commerce.geo.service.GeoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Geografía")
@RestController
@RequestMapping("/api/v1/provincias")
@RequiredArgsConstructor
public class ProvinciaController {

    private final GeoService geoService;

    @Operation(summary = "Listar ciudades de una provincia")
    @GetMapping("/{id}/ciudades")
    public ResponseEntity<List<CiudadResponse>> listarCiudades(@PathVariable Long id) {
        return ResponseEntity.ok(geoService.listarCiudadesDeProvincia(id));
    }
}
