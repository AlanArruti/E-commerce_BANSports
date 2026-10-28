package com.BlandiArruti.E_commerce.envio.service;

import com.BlandiArruti.E_commerce.cliente.entity.Direccion;
import com.BlandiArruti.E_commerce.envio.dto.request.EnvioRequest;
import com.BlandiArruti.E_commerce.envio.dto.request.EstadoEnvioRequest;
import com.BlandiArruti.E_commerce.envio.dto.response.EnvioResponse;
import com.BlandiArruti.E_commerce.envio.entity.Envio;
import com.BlandiArruti.E_commerce.envio.mapper.EnvioMapper;
import com.BlandiArruti.E_commerce.envio.repository.EnvioRepository;
import com.BlandiArruti.E_commerce.enums.EstadoEnvio;
import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.exception.ConflictoException;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.notificacion.service.NotificacionService;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import com.BlandiArruti.E_commerce.pedido.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class EnvioService implements IEnvioService {

    private final EnvioRepository envioRepository;
    private final EnvioMapper envioMapper;
    private final PedidoRepository pedidoRepository;
    private final NotificacionService notificacionService;

    @Transactional(readOnly = true)
    public List<EnvioResponse> listarTodos(EstadoEnvio estado) {
        List<Envio> envios = estado != null
                ? envioRepository.findByEstado(estado)
                : envioRepository.findAll();
        return envios.stream().map(envioMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EnvioResponse buscarPorId(Long id) {
        return envioMapper.toResponse(
                envioRepository.findById(id)
                        .orElseThrow(() -> EntidadNoEncontradaException.envio(id))
        );
    }

    public EnvioResponse crear(Long idPedido, EnvioRequest request) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> EntidadNoEncontradaException.pedido(idPedido));

        if (pedido.getEstadoPedido() != EstadoPedido.PAGADO) {
            throw new EcommerceException(
                    "Solo se puede crear un envío para pedidos en estado PAGADO.");
        }

        if (envioRepository.findByPedidoId(idPedido).isPresent()) {
            throw new ConflictoException("El pedido con id " + idPedido + " ya tiene un envío asignado.");
        }

        Direccion direccion = pedido.getDireccionEnvio();
        if (direccion == null) {
            throw new EcommerceException("El pedido no tiene dirección de envío. El cliente debe indicarla al hacer el checkout.");
        }

        Envio envio = Envio.builder()
                .pedido(pedido)
                .direccion(direccion)
                .estado(EstadoEnvio.DESPACHADO)
                .fechaSalida(request.fechaSalida())
                .fechaLlegada(request.fechaLlegada())
                .build();
        envioRepository.save(envio);

        pedido.setEstadoPedido(EstadoPedido.DESPACHADO);
        pedido.setEnvio(envio);
        pedidoRepository.save(pedido);

        notificacionService.notificarEnvioActualizado(envio);

        return envioMapper.toResponse(envio);
    }

    public EnvioResponse actualizarEstado(Long id, EstadoEnvioRequest request) {
        Envio envio = envioRepository.findById(id)
                .orElseThrow(() -> EntidadNoEncontradaException.envio(id));

        EstadoEnvio actual = envio.getEstado();
        EstadoEnvio nuevo = request.estado();

        boolean valida = switch (actual) {
            case DESPACHADO -> nuevo == EstadoEnvio.EN_CAMINO;
            case EN_CAMINO  -> nuevo == EstadoEnvio.ENTREGADO;
            default         -> false;
        };

        if (!valida) {
            throw new EcommerceException(
                    "Transición de estado de envío inválida: " + actual + " → " + nuevo + ".");
        }

        envio.setEstado(nuevo);

        if (nuevo == EstadoEnvio.ENTREGADO) {
            Pedido pedido = envio.getPedido();
            pedido.setEstadoPedido(EstadoPedido.ENTREGADO);
            pedidoRepository.save(pedido);
        }

        envioRepository.save(envio);
        notificacionService.notificarEnvioActualizado(envio);

        return envioMapper.toResponse(envio);
    }

    public void eliminar(Long id) {
        Envio envio = envioRepository.findById(id)
                .orElseThrow(() -> EntidadNoEncontradaException.envio(id));

        if (envio.getEstado() == EstadoEnvio.ENTREGADO) {
            throw new ConflictoException("No se puede eliminar un envío que ya fue entregado.");
        }

        Pedido pedido = envio.getPedido();
        pedido.setEstadoPedido(EstadoPedido.PAGADO);
        pedido.setEnvio(null);
        pedidoRepository.save(pedido);

        envioRepository.delete(envio);
    }
}
