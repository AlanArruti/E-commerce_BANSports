package com.BlandiArruti.E_commerce.pedido.service;

import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.envio.dto.request.EnvioRequest;
import com.BlandiArruti.E_commerce.envio.dto.request.EstadoEnvioRequest;
import com.BlandiArruti.E_commerce.envio.dto.response.EnvioResponse;
import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.enums.TipoFactura;
import com.BlandiArruti.E_commerce.factura.dto.response.FacturaResponse;
import com.BlandiArruti.E_commerce.pedido.dto.request.EstadoPedidoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.request.PagoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.request.PedidoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.response.PedidoResponse;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import com.BlandiArruti.E_commerce.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface IPedidoService {
    PageResponse<PedidoResponse> listarTodos(EstadoPedido estado, Long clienteId, Pageable pageable);
    PedidoResponse buscarPorId(Long id);
    PedidoResponse crear(PedidoRequest request, UsuarioDetails principal);
    void cancelar(Long id);
    // Para PagoService: el pago con pasarelas (MercadoPago, etc.) vive en el paquete pago/.
    Pedido obtenerParaPagar(Long id);
    void verificarAcceso(Pedido pedido);
    boolean registrarPagoAprobado(Long pedidoId, TipoFactura tipoFactura, BigDecimal montoPagado);
    double calcularTotal(Pedido pedido);

    PedidoResponse pagar(Long id, PagoRequest request);
    PedidoResponse cambiarEstado(Long id, EstadoPedidoRequest request);
    FacturaResponse obtenerFactura(Long idPedido);
    EnvioResponse obtenerEnvio(Long idPedido);
    EnvioResponse crearEnvio(Long idPedido, EnvioRequest request);
    EnvioResponse actualizarEstadoEnvio(Long idPedido, EstadoEnvioRequest request);
}
