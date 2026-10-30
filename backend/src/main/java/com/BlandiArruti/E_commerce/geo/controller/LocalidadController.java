package com.BlandiArruti.E_commerce.geo.controller;

import com.BlandiArruti.E_commerce.geo.dto.response.LocalidadResponse;
import com.BlandiArruti.E_commerce.geo.service.LocalidadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Geografía", description = "Búsqueda de localidades con Georef")
@RestController
@RequestMapping("/api/v1/geo")
@RequiredArgsConstructor
public class LocalidadController {

    private final LocalidadService localidadService;

    @Operation(summary = "Localidades de Argentina mientras se escribe",
               description = "Hasta 10 resultados de Georef. Devuelve lista vacía con menos de 3 caracteres.")
    @GetMapping("/localidades")
    public ResponseEntity<List<LocalidadResponse>> buscar(@RequestParam String texto) {
        return ResponseEntity.ok(localidadService.buscar(texto));
    }
}
