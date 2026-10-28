package com.BlandiArruti.E_commerce.administrador.controller;

import com.BlandiArruti.E_commerce.administrador.dto.request.AdministradorRequest;
import com.BlandiArruti.E_commerce.administrador.dto.response.AdministradorResponse;
import com.BlandiArruti.E_commerce.administrador.service.IAdministradorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Administradores", description = "Gestión de cuentas de administrador (solo ADMIN)")
@RestController
@RequestMapping("/api/v1/administrador")
@RequiredArgsConstructor
public class AdministradorController {

    private final IAdministradorService administradorService;

    @Operation(summary = "Listar administradores")
    @GetMapping
    public ResponseEntity<List<AdministradorResponse>> listarTodos() {
        return ResponseEntity.ok(administradorService.listarTodos());
    }

    @Operation(summary = "Buscar administrador por ID")
    @GetMapping("/{id}")
    public ResponseEntity<AdministradorResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(administradorService.buscarPorId(id));
    }

    @Operation(summary = "Crear administrador")
    @PostMapping
    public ResponseEntity<AdministradorResponse> crear(@Valid @RequestBody AdministradorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(administradorService.crear(request));
    }

    @Operation(summary = "Actualizar administrador")
    @PutMapping("/{id}")
    public ResponseEntity<AdministradorResponse> actualizar(@PathVariable Long id,
                                                            @Valid @RequestBody AdministradorRequest request) {
        return ResponseEntity.ok(administradorService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar administrador")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        administradorService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
