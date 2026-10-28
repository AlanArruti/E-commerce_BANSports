package com.BlandiArruti.E_commerce.carrito.controller;

import com.BlandiArruti.E_commerce.carrito.dto.request.CantidadCarritoRequest;
import com.BlandiArruti.E_commerce.carrito.dto.request.CheckoutRequest;
import com.BlandiArruti.E_commerce.carrito.dto.request.ItemCarritoRequest;
import com.BlandiArruti.E_commerce.carrito.dto.response.CarritoResponse;
import com.BlandiArruti.E_commerce.carrito.service.CarritoService;
import com.BlandiArruti.E_commerce.pedido.dto.response.PedidoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Carrito", description = "Gestión del carrito de compras del cliente")
@RestController
@RequestMapping("/api/v1/cliente/{idCliente}/carrito")
@RequiredArgsConstructor
public class CarritoController {

    private final CarritoService carritoService;

    @Operation(summary = "Obtener carrito del cliente")
    @GetMapping
    public ResponseEntity<CarritoResponse> obtenerCarrito(@PathVariable Long idCliente) {
        return ResponseEntity.ok(carritoService.obtenerCarrito(idCliente));
    }

    @Operation(summary = "Agregar producto al carrito", description = "idVariante es opcional: si el producto tiene una sola variante activa se autoselecciona.")
    @PostMapping("/items")
    public ResponseEntity<CarritoResponse> agregarItem(@PathVariable Long idCliente,
                                                        @Valid @RequestBody ItemCarritoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(carritoService.agregarItem(idCliente, request));
    }

    @Operation(summary = "Actualizar cantidad de un ítem del carrito")
    @PutMapping("/items/{idItem}")
    public ResponseEntity<CarritoResponse> actualizarCantidad(@PathVariable Long idCliente,
                                                               @PathVariable Long idItem,
                                                               @Valid @RequestBody CantidadCarritoRequest request) {
        return ResponseEntity.ok(carritoService.actualizarCantidad(idCliente, idItem, request));
    }

    @Operation(summary = "Eliminar ítem del carrito")
    @DeleteMapping("/items/{idItem}")
    public ResponseEntity<CarritoResponse> eliminarItem(@PathVariable Long idCliente,
                                                         @PathVariable Long idItem) {
        return ResponseEntity.ok(carritoService.eliminarItem(idCliente, idItem));
    }

    @Operation(summary = "Vaciar el carrito")
    @DeleteMapping
    public ResponseEntity<Void> vaciarCarrito(@PathVariable Long idCliente) {
        carritoService.vaciarCarrito(idCliente);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Confirmar pedido desde el carrito", description = "Crea el pedido con todos los ítems del carrito y lo vacía. El cliente indica la dirección de envío.")
    @PostMapping("/checkout")
    public ResponseEntity<PedidoResponse> checkout(@PathVariable Long idCliente,
                                                    @Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(carritoService.checkout(idCliente, request.idDireccion()));
    }
}
