package com.BlandiArruti.E_commerce.cupon.controller;

import com.BlandiArruti.E_commerce.cupon.dto.request.CuponRequest;
import com.BlandiArruti.E_commerce.cupon.dto.response.CuponResponse;
import com.BlandiArruti.E_commerce.cupon.dto.response.ValidacionCuponResponse;
import com.BlandiArruti.E_commerce.cupon.service.ICuponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Seguridad (SecurityConfig): validar es del CLIENTE; todo lo demás, solo ADMIN. */
@Tag(name = "Cupones", description = "Cupones de descuento sobre el total del pedido")
@RestController
@RequestMapping("/api/v1/cupones")
@RequiredArgsConstructor
public class CuponController {

    private final ICuponService cuponService;

    @Operation(summary = "Validar un cupón para el carrito (CLIENTE)",
            description = "Usa el carrito del cliente logueado. Devuelve el descuento o 400 con el motivo "
                    + "(no existe, vencido, agotado, compra mínima). No consume usos.")
    @GetMapping("/validar")
    public ResponseEntity<ValidacionCuponResponse> validar(@RequestParam String codigo) {
        return ResponseEntity.ok(cuponService.validarParaCarrito(codigo));
    }

    @Operation(summary = "Listar cupones (ADMIN)", description = "Más nuevos primero, con su estado a hoy.")
    @GetMapping
    public ResponseEntity<List<CuponResponse>> listar() {
        return ResponseEntity.ok(cuponService.listar());
    }

    @Operation(summary = "Buscar cupón por ID (ADMIN)")
    @GetMapping("/{id}")
    public ResponseEntity<CuponResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(cuponService.buscarPorId(id));
    }

    @Operation(summary = "Crear cupón (ADMIN)")
    @PostMapping
    public ResponseEntity<CuponResponse> crear(@Valid @RequestBody CuponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuponService.crear(request));
    }

    @Operation(summary = "Editar cupón (ADMIN)")
    @PutMapping("/{id}")
    public ResponseEntity<CuponResponse> actualizar(@PathVariable Long id, @Valid @RequestBody CuponRequest request) {
        return ResponseEntity.ok(cuponService.actualizar(id, request));
    }

    @Operation(summary = "Dar de baja un cupón (ADMIN)", description = "Baja lógica: los pedidos que lo usaron no cambian.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        cuponService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
