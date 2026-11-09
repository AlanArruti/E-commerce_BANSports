package com.BlandiArruti.E_commerce.resena.controller;

import com.BlandiArruti.E_commerce.resena.dto.request.ResenaRequest;
import com.BlandiArruti.E_commerce.resena.dto.response.PermisoResenaResponse;
import com.BlandiArruti.E_commerce.resena.dto.response.ResenaResponse;
import com.BlandiArruti.E_commerce.resena.service.ResenaService;
import com.BlandiArruti.E_commerce.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reseñas", description = "Opiniones y valoraciones de los productos")
@RestController
@RequestMapping("/api/v1/producto/{idProducto}/resenas")
@RequiredArgsConstructor
public class ResenaController {

    private final ResenaService resenaService;

    @Operation(summary = "Listar reseñas del producto", description = "Público. De la más nueva a la más vieja.")
    @GetMapping
    public ResponseEntity<PageResponse<ResenaResponse>> listar(
            @PathVariable Long idProducto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(resenaService.listar(
                idProducto, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by(Sort.Direction.DESC, "fecha"))));
    }

    @Operation(summary = "¿Puedo opinar?",
               description = "Público. Sin sesión responde SIN_SESION; con sesión dice si el cliente compró el producto o si ya opinó.")
    @GetMapping("/permiso")
    public ResponseEntity<PermisoResenaResponse> permiso(@PathVariable Long idProducto) {
        return ResponseEntity.ok(resenaService.permiso(idProducto));
    }

    @Operation(summary = "Dejar una reseña (solo CLIENTE que compró el producto)",
               description = "Una por cliente y producto. 403 si no lo compró, 409 si ya opinó.")
    @PostMapping
    public ResponseEntity<ResenaResponse> crear(@PathVariable Long idProducto,
                                                @Valid @RequestBody ResenaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resenaService.crear(idProducto, request));
    }

    @Operation(summary = "Eliminar una reseña (moderación, solo ADMIN)")
    @DeleteMapping("/{idResena}")
    public ResponseEntity<Void> eliminar(@PathVariable Long idProducto, @PathVariable Long idResena) {
        resenaService.eliminar(idProducto, idResena);
        return ResponseEntity.noContent().build();
    }
}
