package com.BlandiArruti.E_commerce.geo.controller;

import com.BlandiArruti.E_commerce.geo.dto.response.LugarResponse;
import com.BlandiArruti.E_commerce.geo.dto.response.SugerenciaDireccionResponse;
import com.BlandiArruti.E_commerce.geo.service.LugarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Geografía", description = "Búsqueda de direcciones con Google Maps")
@RestController
@RequestMapping("/api/v1/geo")
@RequiredArgsConstructor
public class LugarController {

    private final LugarService lugarService;

    @Operation(summary = "Sugerencias de direcciones mientras se escribe",
               description = "Devuelve lista vacía con menos de 3 caracteres. `sesion` es un id que el front "
                           + "genera por búsqueda y repite en el detalle, para que Google la cobre como una sola.")
    @GetMapping("/sugerencias")
    public ResponseEntity<List<SugerenciaDireccionResponse>> sugerir(
            @RequestParam String texto,
            @RequestParam(required = false) String sesion) {
        return ResponseEntity.ok(lugarService.sugerir(texto, sesion));
    }

    @Operation(summary = "Detalle de una dirección elegida",
               description = "Calle, altura, código postal, ciudad y provincia para precargar el formulario.")
    @GetMapping("/lugares/{placeId}")
    public ResponseEntity<LugarResponse> obtener(
            @PathVariable String placeId,
            @RequestParam(required = false) String sesion) {
        return ResponseEntity.ok(lugarService.obtener(placeId, sesion));
    }
}
