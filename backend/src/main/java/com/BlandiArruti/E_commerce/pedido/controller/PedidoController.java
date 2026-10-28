package com.BlandiArruti.E_commerce.pedido.controller;

import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.envio.dto.request.EnvioRequest;
import com.BlandiArruti.E_commerce.envio.dto.request.EstadoEnvioRequest;
import com.BlandiArruti.E_commerce.envio.dto.response.EnvioResponse;
import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.factura.dto.response.FacturaResponse;
import com.BlandiArruti.E_commerce.mercadopago.dto.response.PreferenciaResponse;
import com.BlandiArruti.E_commerce.pedido.dto.request.EstadoPedidoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.request.PagoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.request.PedidoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.response.PedidoResponse;
import com.BlandiArruti.E_commerce.pedido.service.IPedidoService;
import com.BlandiArruti.E_commerce.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Pedidos", description = "Gestión de pedidos, pagos y envíos")
@RestController
@RequestMapping("/api/v1/pedido")
@RequiredArgsConstructor
public class PedidoController {

    private final IPedidoService pedidoService;

    @Operation(summary = "Listar pedidos", description = "Filtrables por estado y cliente. Un CLIENTE solo ve los suyos.")
    @GetMapping
    public ResponseEntity<PageResponse<PedidoResponse>> listarTodos(
            @RequestParam(required = false) EstadoPedido estado,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(pedidoService.listarTodos(estado, clienteId, PageRequest.of(page, size)));
    }

    @Operation(summary = "Buscar pedido por ID")
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.buscarPorId(id));
    }

    @Operation(summary = "Crear pedido")
    @PostMapping
    public ResponseEntity<PedidoResponse> crear(
            @Valid @RequestBody PedidoRequest request,
            @AuthenticationPrincipal UsuarioDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.crear(request, principal));
    }

    @Operation(summary = "Cancelar pedido")
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        pedidoService.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Iniciar pago con MercadoPago", description = "Devuelve el initPoint para redirigir al cliente a la página de pago.")
    @PostMapping("/{id}/iniciar-pago")
    public ResponseEntity<PreferenciaResponse> iniciarPago(@PathVariable Long id,
                                                           @Valid @RequestBody PagoRequest request) {
        return ResponseEntity.ok(pedidoService.iniciarPagoMercadoPago(id, request));
    }

    @Operation(summary = "Pagar pedido directamente (sin MercadoPago)")
    @PatchMapping("/{id}/pagar")
    public ResponseEntity<PedidoResponse> pagar(@PathVariable Long id,
                                                @Valid @RequestBody PagoRequest request) {
        return ResponseEntity.ok(pedidoService.pagar(id, request));
    }

    @Operation(summary = "Cambiar estado del pedido (solo ADMIN)", description = "Flujo: PENDIENTE_PAGO → EN_PREPARACION → PAGADO → DESPACHADO → ENTREGADO")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<PedidoResponse> cambiarEstado(@PathVariable Long id,
                                                        @Valid @RequestBody EstadoPedidoRequest request) {
        return ResponseEntity.ok(pedidoService.cambiarEstado(id, request));
    }

    @Operation(summary = "Obtener factura del pedido")
    @GetMapping("/{id}/factura")
    public ResponseEntity<FacturaResponse> obtenerFactura(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtenerFactura(id));
    }

    @Operation(summary = "Obtener envío del pedido")
    @GetMapping("/{id}/envio")
    public ResponseEntity<EnvioResponse> obtenerEnvio(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtenerEnvio(id));
    }

    @Operation(summary = "Crear envío del pedido (solo ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/envio")
    public ResponseEntity<EnvioResponse> crearEnvio(@PathVariable Long id,
                                                    @Valid @RequestBody EnvioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.crearEnvio(id, request));
    }

    @Operation(summary = "Actualizar estado del envío (solo ADMIN)", description = "Transiciones: DESPACHADO → EN_CAMINO → ENTREGADO")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/envio/estado")
    public ResponseEntity<EnvioResponse> actualizarEstadoEnvio(@PathVariable Long id,
                                                               @Valid @RequestBody EstadoEnvioRequest request) {
        return ResponseEntity.ok(pedidoService.actualizarEstadoEnvio(id, request));
    }
}
