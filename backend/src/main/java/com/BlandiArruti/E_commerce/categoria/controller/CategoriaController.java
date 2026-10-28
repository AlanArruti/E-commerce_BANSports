package com.BlandiArruti.E_commerce.categoria.controller;

import com.BlandiArruti.E_commerce.categoria.dto.request.CategoriaRequest;
import com.BlandiArruti.E_commerce.categoria.dto.response.CategoriaResponse;
import com.BlandiArruti.E_commerce.categoria.dto.response.EliminacionResponse;
import com.BlandiArruti.E_commerce.categoria.service.ICategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Categorías", description = "Gestión de categorías de productos")
@RestController
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final ICategoriaService categoriaService;

    @Operation(summary = "Listar categorías", description = "Filtrables por nombre.")
    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listarTodas(
            @RequestParam(required = false) String nombre) {
        return ResponseEntity.ok(categoriaService.listarTodas(nombre));
    }

    @Operation(summary = "Buscar categoría por ID")
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaService.buscarPorId(id));
    }

    @Operation(summary = "Crear categoría")
    @PostMapping
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.crear(request));
    }

    @Operation(summary = "Actualizar categoría")
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.ok(categoriaService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar categoría", description = "Sin confirmar=true devuelve aviso de productos afectados. Con confirmar=true desvincula y elimina.")
    @DeleteMapping("/{id}")
    public ResponseEntity<EliminacionResponse> eliminar(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean confirmar) {
        return ResponseEntity.ok(categoriaService.eliminar(id, confirmar));
    }
}
