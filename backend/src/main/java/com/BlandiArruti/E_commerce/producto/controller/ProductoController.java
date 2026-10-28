package com.BlandiArruti.E_commerce.producto.controller;

import com.BlandiArruti.E_commerce.producto.dto.request.ProductoRequest;
import com.BlandiArruti.E_commerce.producto.dto.request.StockRequest;
import com.BlandiArruti.E_commerce.producto.dto.request.VarianteRequest;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.producto.dto.response.ImagenProductoResponse;
import com.BlandiArruti.E_commerce.producto.dto.response.ProductoResponse;
import com.BlandiArruti.E_commerce.producto.dto.response.VarianteResponse;
import com.BlandiArruti.E_commerce.producto.entity.ImagenProducto;
import com.BlandiArruti.E_commerce.producto.service.IProductoService;
import com.BlandiArruti.E_commerce.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Tag(name = "Productos", description = "Gestión de productos y sus variantes")
@RestController
@RequestMapping("/api/v1/producto")
@RequiredArgsConstructor
public class ProductoController {

    private final IProductoService productoService;

    /** Campos por los que se puede ordenar el catálogo (whitelist). */
    private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
            "id", "id",
            "nombre", "nombre",
            "precio", "precio"
    );

    @Operation(summary = "Listar productos",
               description = "Filtrables por categoría, precio y nombre. Ordenable por id, nombre o precio.")
    @GetMapping
    public ResponseEntity<PageResponse<ProductoResponse>> listarTodos(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Double precioMin,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(productoService.listarTodos(
                categoriaId, precioMin, precioMax, search, PageRequest.of(page, size, ordenar(sortBy, sortDir))));
    }

    private Sort ordenar(String sortBy, String sortDir) {
        String campo = CAMPOS_ORDENABLES.get(sortBy == null ? "" : sortBy.toLowerCase());
        if (campo == null) {
            throw new EcommerceException(
                    "No se puede ordenar por '" + sortBy + "'. Campos válidos: id, nombre, precio.");
        }
        Sort.Direction direccion = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direccion, campo);
    }

    @Operation(summary = "Subir/reemplazar la imagen del producto (solo ADMIN)",
               description = "Acepta JPEG, PNG o WEBP de hasta 2 MB.")
    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImagenProductoResponse> subirImagen(@PathVariable Long id,
                                                              @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.subirImagen(id, archivo));
    }

    @Operation(summary = "Obtener la imagen del producto", description = "Público. Devuelve los bytes de la imagen.")
    @GetMapping("/{id}/imagen")
    public ResponseEntity<byte[]> obtenerImagen(@PathVariable Long id) {
        ImagenProducto imagen = productoService.obtenerImagen(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagen.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(imagen.getDatos());
    }

    @Operation(summary = "Eliminar la imagen del producto (solo ADMIN)")
    @DeleteMapping("/{id}/imagen")
    public ResponseEntity<Void> eliminarImagen(@PathVariable Long id) {
        productoService.eliminarImagen(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Buscar producto por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.buscarPorId(id));
    }

    @Operation(summary = "Crear producto (solo ADMIN)")
    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(request));
    }

    @Operation(summary = "Actualizar producto (solo ADMIN)")
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(productoService.actualizar(id, request));
    }

    @Operation(summary = "Dar de baja producto (soft delete, solo ADMIN)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar variantes del producto")
    @GetMapping("/{id}/variante")
    public ResponseEntity<List<VarianteResponse>> listarVariantes(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.listarVariantes(id));
    }

    @Operation(summary = "Agregar variante al producto (solo ADMIN)")
    @PostMapping("/{id}/variante")
    public ResponseEntity<VarianteResponse> agregarVariante(@PathVariable Long id,
                                                            @Valid @RequestBody VarianteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.agregarVariante(id, request));
    }

    @Operation(summary = "Actualizar variante (solo ADMIN)")
    @PutMapping("/{idProducto}/variante/{idVariante}")
    public ResponseEntity<VarianteResponse> actualizarVariante(@PathVariable Long idProducto,
                                                               @PathVariable Long idVariante,
                                                               @Valid @RequestBody VarianteRequest request) {
        return ResponseEntity.ok(productoService.actualizarVariante(idProducto, idVariante, request));
    }

    @Operation(summary = "Eliminar variante (solo ADMIN)")
    @DeleteMapping("/{idProducto}/variante/{idVariante}")
    public ResponseEntity<Void> eliminarVariante(@PathVariable Long idProducto,
                                                 @PathVariable Long idVariante) {
        productoService.eliminarVariante(idProducto, idVariante);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ajustar stock de variante (solo ADMIN)", description = "Operaciones: AGREGAR o REDUCIR.")
    @PatchMapping("/{idProducto}/variante/{idVariante}/stock")
    public ResponseEntity<VarianteResponse> ajustarStock(@PathVariable Long idProducto,
                                                         @PathVariable Long idVariante,
                                                         @Valid @RequestBody StockRequest request) {
        return ResponseEntity.ok(productoService.ajustarStock(idProducto, idVariante, request));
    }
}
