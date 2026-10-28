package com.BlandiArruti.E_commerce.pedido.service;

import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.envio.dto.request.EnvioRequest;
import com.BlandiArruti.E_commerce.envio.dto.request.EstadoEnvioRequest;
import com.BlandiArruti.E_commerce.envio.dto.response.EnvioResponse;
import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.enums.TipoFactura;
import com.BlandiArruti.E_commerce.factura.dto.response.FacturaResponse;
import com.BlandiArruti.E_commerce.mercadopago.dto.response.PreferenciaResponse;
import com.BlandiArruti.E_commerce.pedido.dto.request.EstadoPedidoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.request.PagoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.request.PedidoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.response.PedidoResponse;
import com.BlandiArruti.E_commerce.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface IPedidoService {
    PageResponse<PedidoResponse> listarTodos(EstadoPedido estado, Long clienteId, Pageable pageable);
    PedidoResponse buscarPorId(Long id);
    PedidoResponse crear(PedidoRequest request, UsuarioDetails principal);
    void cancelar(Long id);
    PreferenciaResponse iniciarPagoMercadoPago(Long id, PagoRequest request);
    void confirmarPagoMercadoPago(Long pedidoId, TipoFactura tipoFactura);
    PedidoResponse pagar(Long id, PagoRequest request);
    PedidoResponse cambiarEstado(Long id, EstadoPedidoRequest request);
    FacturaResponse obtenerFactura(Long idPedido);
    EnvioResponse obtenerEnvio(Long idPedido);
    EnvioResponse crearEnvio(Long idPedido, EnvioRequest request);
    EnvioResponse actualizarEstadoEnvio(Long idPedido, EstadoEnvioRequest request);
}
