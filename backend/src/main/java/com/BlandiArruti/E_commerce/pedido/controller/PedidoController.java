package com.BlandiArruti.E_commerce.pedido.controller;

import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.envio.dto.request.EnvioRequest;
import com.BlandiArruti.E_commerce.envio.dto.request.EstadoEnvioRequest;
import com.BlandiArruti.E_commerce.envio.dto.response.EnvioResponse;
import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.factura.dto.response.FacturaPdfResponse;
import com.BlandiArruti.E_commerce.factura.dto.response.FacturaResponse;
import com.BlandiArruti.E_commerce.pedido.dto.request.ConfirmarRecepcionRequest;
import com.BlandiArruti.E_commerce.pedido.dto.request.EstadoPedidoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.request.PagoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.request.PedidoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.request.ResolucionReembolsoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.request.SolicitudReembolsoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.response.CancelacionPedidoResponse;
import com.BlandiArruti.E_commerce.pedido.dto.response.PedidoResponse;
import com.BlandiArruti.E_commerce.pedido.service.IPedidoService;
import com.BlandiArruti.E_commerce.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
        // Los más nuevos primero; el id desempata pedidos creados en el mismo instante.
        Sort orden = Sort.by(Sort.Direction.DESC, "fechaCreacion").and(Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(pedidoService.listarTodos(estado, clienteId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), orden)));
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

    @Operation(summary = "Cancelar pedido",
            description = "RF04: el cliente lo cancela mientras no esté despachado. Si ya estaba cobrado con "
                    + "Mercado Pago, se devuelve la plata antes de cancelarlo (si la devolución falla, no se cancela).")
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<CancelacionPedidoResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.cancelar(id));
    }

    // El cliente paga con una pasarela: ver PagoController (/api/v1/pago).
    // Solo ADMIN: marca el pedido como pagado sin cobrar. Si lo pudiera llamar el cliente,
    // se saltearía la pasarela y se llevaría la compra gratis.
    @Operation(summary = "Marcar pedido como pagado sin MercadoPago (solo ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
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

    @Operation(summary = "Confirmar si llegó el pedido entregado (solo el cliente dueño)",
            description = "RF03. { recibido: true|false, comentario? }. Solo con el pedido ENTREGADO. "
                    + "\"No lo recibí\" se puede cambiar después a \"Lo recibí\"; \"Lo recibí\" es definitivo.")
    @PatchMapping("/{id}/recepcion")
    public ResponseEntity<PedidoResponse> confirmarRecepcion(@PathVariable Long id,
                                                             @Valid @RequestBody ConfirmarRecepcionRequest request) {
        return ResponseEntity.ok(pedidoService.confirmarRecepcion(id, request));
    }

    @Operation(summary = "Pedir el reembolso de un pedido entregado (solo el cliente dueño)",
            description = "RF05. { motivo }. Hasta 10 días después de la entrega y una sola vez. "
                    + "Queda SOLICITADO hasta que lo resuelva el admin.")
    @PostMapping("/{id}/reembolso")
    public ResponseEntity<PedidoResponse> solicitarReembolso(@PathVariable Long id,
                                                             @Valid @RequestBody SolicitudReembolsoRequest request) {
        return ResponseEntity.ok(pedidoService.solicitarReembolso(id, request));
    }

    @Operation(summary = "Aprobar o rechazar el reembolso pedido (solo ADMIN)",
            description = "RF05. { aprobar, respuesta? }. Al aprobar se devuelve la plata en Mercado Pago "
                    + "(si falla, sigue SOLICITADO). Al rechazar, la respuesta es obligatoria. El stock no vuelve.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/reembolso")
    public ResponseEntity<PedidoResponse> resolverReembolso(@PathVariable Long id,
                                                            @Valid @RequestBody ResolucionReembolsoRequest request) {
        return ResponseEntity.ok(pedidoService.resolverReembolso(id, request));
    }

    @Operation(summary = "Obtener factura del pedido")
    @GetMapping("/{id}/factura")
    public ResponseEntity<FacturaResponse> obtenerFactura(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtenerFactura(id));
    }

    @Operation(summary = "Descargar la factura del pedido en PDF")
    @GetMapping(value = "/{id}/factura/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarFacturaPdf(@PathVariable Long id) {
        FacturaPdfResponse pdf = pedidoService.obtenerFacturaPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(pdf.nombreArchivo()).build().toString())
                .body(pdf.contenido());
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
