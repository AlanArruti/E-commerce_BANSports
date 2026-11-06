package com.BlandiArruti.E_commerce.favorito.controller;

import com.BlandiArruti.E_commerce.favorito.dto.response.FavoritoResponse;
import com.BlandiArruti.E_commerce.favorito.service.FavoritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Favoritos", description = "Productos que el cliente guardó para comprar más adelante")
@RestController
@RequestMapping("/api/v1/cliente/{idCliente}/favoritos")
@RequiredArgsConstructor
public class FavoritoController {

    private final FavoritoService favoritoService;

    @Operation(summary = "Listar favoritos del cliente", description = "No incluye los productos dados de baja.")
    @GetMapping
    public ResponseEntity<List<FavoritoResponse>> listar(@PathVariable Long idCliente) {
        return ResponseEntity.ok(favoritoService.listar(idCliente));
    }

    @Operation(summary = "Agregar producto a favoritos", description = "Si ya estaba guardado, lo devuelve sin duplicarlo.")
    @PostMapping("/{idProducto}")
    public ResponseEntity<FavoritoResponse> agregar(@PathVariable Long idCliente, @PathVariable Long idProducto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(favoritoService.agregar(idCliente, idProducto));
    }

    @Operation(summary = "Quitar producto de favoritos")
    @DeleteMapping("/{idProducto}")
    public ResponseEntity<Void> quitar(@PathVariable Long idCliente, @PathVariable Long idProducto) {
        favoritoService.quitar(idCliente, idProducto);
        return ResponseEntity.noContent().build();
    }
}
