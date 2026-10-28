package com.BlandiArruti.E_commerce.cliente.controller;

import com.BlandiArruti.E_commerce.cliente.dto.request.CambioPasswordRequest;
import com.BlandiArruti.E_commerce.cliente.dto.request.ClienteRequest;
import com.BlandiArruti.E_commerce.cliente.dto.request.ClienteUpdateRequest;
import com.BlandiArruti.E_commerce.cliente.dto.request.DireccionRequest;
import com.BlandiArruti.E_commerce.cliente.dto.response.ClienteResponse;
import com.BlandiArruti.E_commerce.cliente.dto.response.DireccionResponse;
import com.BlandiArruti.E_commerce.cliente.service.IClienteService;
import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.pedido.dto.response.PedidoResponse;
import com.BlandiArruti.E_commerce.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Clientes", description = "Gestión de clientes y sus direcciones")
@RestController
@RequestMapping("/api/v1/cliente")
@RequiredArgsConstructor
public class ClienteController {

    private final IClienteService clienteService;

    @Operation(summary = "Listar todos los clientes (solo ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<PageResponse<ClienteResponse>> listarTodos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(clienteService.listarTodos(PageRequest.of(page, size)));
    }

    @Operation(summary = "Buscar cliente por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    @Operation(summary = "Crear cliente (solo ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crear(request));
    }

    @Operation(summary = "Actualizar datos del cliente",
               description = "Edita nombre, apellido y email. La contraseña se cambia por separado.")
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponse> actualizar(@PathVariable Long id,
                                                      @Valid @RequestBody ClienteUpdateRequest request) {
        return ResponseEntity.ok(clienteService.actualizar(id, request));
    }

    @Operation(summary = "Cambiar contraseña", description = "Requiere la contraseña actual.")
    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> cambiarPassword(@PathVariable Long id,
                                                @Valid @RequestBody CambioPasswordRequest request) {
        clienteService.cambiarPassword(id, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Dar de baja cliente (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar direcciones del cliente")
    @GetMapping("/{id}/direccion")
    public ResponseEntity<List<DireccionResponse>> listarDirecciones(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.listarDirecciones(id));
    }

    @Operation(summary = "Agregar dirección al cliente")
    @PostMapping("/{id}/direccion")
    public ResponseEntity<DireccionResponse> agregarDireccion(@PathVariable Long id,
                                                              @Valid @RequestBody DireccionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.agregarDireccion(id, request));
    }

    @Operation(summary = "Eliminar dirección del cliente")
    @DeleteMapping("/{idCliente}/direccion/{idDireccion}")
    public ResponseEntity<Void> eliminarDireccion(@PathVariable Long idCliente,
                                                  @PathVariable Long idDireccion) {
        clienteService.eliminarDireccion(idCliente, idDireccion);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Historial de pedidos del cliente", description = "Filtrable por estado.")
    @GetMapping("/{id}/pedido")
    public ResponseEntity<List<PedidoResponse>> historialPedidos(@PathVariable Long id,
                                                                 @RequestParam(required = false) EstadoPedido estado) {
        return ResponseEntity.ok(clienteService.historialPedidos(id, estado));
    }
}
