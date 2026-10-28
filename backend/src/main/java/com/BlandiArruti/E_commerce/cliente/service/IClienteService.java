package com.BlandiArruti.E_commerce.cliente.service;

import com.BlandiArruti.E_commerce.cliente.dto.request.CambioPasswordRequest;
import com.BlandiArruti.E_commerce.cliente.dto.request.ClienteRequest;
import com.BlandiArruti.E_commerce.cliente.dto.request.ClienteUpdateRequest;
import com.BlandiArruti.E_commerce.cliente.dto.request.DireccionRequest;
import com.BlandiArruti.E_commerce.cliente.dto.response.ClienteResponse;
import com.BlandiArruti.E_commerce.cliente.dto.response.DireccionResponse;
import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.pedido.dto.response.PedidoResponse;
import com.BlandiArruti.E_commerce.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IClienteService {
    PageResponse<ClienteResponse> listarTodos(Pageable pageable);
    ClienteResponse buscarPorId(Long id);
    ClienteResponse crear(ClienteRequest request);
    ClienteResponse actualizar(Long id, ClienteUpdateRequest request);
    void cambiarPassword(Long id, CambioPasswordRequest request);
    void eliminar(Long id);
    List<DireccionResponse> listarDirecciones(Long idCliente);
    DireccionResponse agregarDireccion(Long idCliente, DireccionRequest request);
    void eliminarDireccion(Long idCliente, Long idDireccion);
    List<PedidoResponse> historialPedidos(Long idCliente, EstadoPedido estado);
}
