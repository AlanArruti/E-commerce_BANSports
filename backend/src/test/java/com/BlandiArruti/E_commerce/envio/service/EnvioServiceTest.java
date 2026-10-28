package com.BlandiArruti.E_commerce.envio.service;

import com.BlandiArruti.E_commerce.cliente.entity.Direccion;
import com.BlandiArruti.E_commerce.envio.dto.request.EnvioRequest;
import com.BlandiArruti.E_commerce.envio.dto.request.EstadoEnvioRequest;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnvioServiceTest {

    @Mock EnvioRepository envioRepository;
    @Mock EnvioMapper envioMapper;
    @Mock PedidoRepository pedidoRepository;
    @Mock NotificacionService notificacionService;

    @InjectMocks
    EnvioService envioService;

    // ─── crear ───────────────────────────────────────────────────────────────

    @Test
    void crear_pedidoNoPagado_lanzaEcommerceException() {
        Pedido pedido = Pedido.builder().id(1L).estadoPedido(EstadoPedido.PENDIENTE_PAGO).build();

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() ->
                envioService.crear(1L, new EnvioRequest(LocalDate.now(), LocalDate.now().plusDays(3))))
                .isInstanceOf(EcommerceException.class);

        verify(envioRepository, never()).save(any());
    }

    @Test
    void crear_yaExisteEnvio_lanzaConflictoException() {
        Pedido pedido = Pedido.builder().id(1L).estadoPedido(EstadoPedido.PAGADO).build();
        Envio envioExistente = Envio.builder().id(99L).build();

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(envioRepository.findByPedidoId(1L)).thenReturn(Optional.of(envioExistente));

        assertThatThrownBy(() ->
                envioService.crear(1L, new EnvioRequest(LocalDate.now(), LocalDate.now().plusDays(3))))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void crear_exitoso_guardaEnvio_actualizaPedidoYNotifica() {
        Direccion direccion = Direccion.builder().id(1L).build();
        Pedido pedido = Pedido.builder().id(1L).estadoPedido(EstadoPedido.PAGADO).direccionEnvio(direccion).build();

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(envioRepository.findByPedidoId(1L)).thenReturn(Optional.empty());
        when(envioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(envioMapper.toResponse(any())).thenReturn(null);

        envioService.crear(1L, new EnvioRequest(LocalDate.now(), LocalDate.now().plusDays(3)));

        assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.DESPACHADO);
        verify(pedidoRepository).save(pedido);
        verify(notificacionService).notificarEnvioActualizado(any());
    }

    // ─── actualizarEstado ─────────────────────────────────────────────────────

    @Test
    void actualizarEstado_despachadoAEnCamino_transicionValida() {
        Pedido pedido = Pedido.builder().id(1L).estadoPedido(EstadoPedido.DESPACHADO).build();
        Envio envio = Envio.builder().id(1L).estado(EstadoEnvio.DESPACHADO).pedido(pedido).build();

        when(envioRepository.findById(1L)).thenReturn(Optional.of(envio));
        when(envioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(envioMapper.toResponse(any())).thenReturn(null);

        envioService.actualizarEstado(1L, new EstadoEnvioRequest(EstadoEnvio.EN_CAMINO));

        assertThat(envio.getEstado()).isEqualTo(EstadoEnvio.EN_CAMINO);
        verify(notificacionService).notificarEnvioActualizado(envio);
    }

    @Test
    void actualizarEstado_enCaminoAEntregado_actualizaPedidoTambien() {
        Pedido pedido = Pedido.builder().id(1L).estadoPedido(EstadoPedido.DESPACHADO).build();
        Envio envio = Envio.builder().id(1L).estado(EstadoEnvio.EN_CAMINO).pedido(pedido).build();

        when(envioRepository.findById(1L)).thenReturn(Optional.of(envio));
        when(envioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(envioMapper.toResponse(any())).thenReturn(null);

        envioService.actualizarEstado(1L, new EstadoEnvioRequest(EstadoEnvio.ENTREGADO));

        assertThat(envio.getEstado()).isEqualTo(EstadoEnvio.ENTREGADO);
        assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.ENTREGADO);
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void actualizarEstado_transicionInvalida_lanzaEcommerceException() {
        Pedido pedido = Pedido.builder().id(1L).estadoPedido(EstadoPedido.DESPACHADO).build();
        Envio envio = Envio.builder().id(1L).estado(EstadoEnvio.DESPACHADO).pedido(pedido).build();

        when(envioRepository.findById(1L)).thenReturn(Optional.of(envio));

        assertThatThrownBy(() ->
                envioService.actualizarEstado(1L, new EstadoEnvioRequest(EstadoEnvio.ENTREGADO)))
                .isInstanceOf(EcommerceException.class);
    }

    @Test
    void actualizarEstado_desdeEntregado_lanzaEcommerceException() {
        Pedido pedido = Pedido.builder().id(1L).estadoPedido(EstadoPedido.ENTREGADO).build();
        Envio envio = Envio.builder().id(1L).estado(EstadoEnvio.ENTREGADO).pedido(pedido).build();

        when(envioRepository.findById(1L)).thenReturn(Optional.of(envio));

        assertThatThrownBy(() ->
                envioService.actualizarEstado(1L, new EstadoEnvioRequest(EstadoEnvio.EN_CAMINO)))
                .isInstanceOf(EcommerceException.class);
    }

    // ─── eliminar ────────────────────────────────────────────────────────────

    @Test
    void eliminar_envioEntregado_lanzaConflicto() {
        Pedido pedido = Pedido.builder().id(1L).estadoPedido(EstadoPedido.ENTREGADO).build();
        Envio envio = Envio.builder().id(1L).estado(EstadoEnvio.ENTREGADO).pedido(pedido).build();

        when(envioRepository.findById(1L)).thenReturn(Optional.of(envio));

        assertThatThrownBy(() -> envioService.eliminar(1L))
                .isInstanceOf(ConflictoException.class);

        verify(envioRepository, never()).delete(any());
    }

    @Test
    void eliminar_envioNoEntregado_reviertePedidoAPagadoYElimina() {
        Pedido pedido = Pedido.builder().id(1L).estadoPedido(EstadoPedido.DESPACHADO).build();
        Envio envio = Envio.builder().id(1L).estado(EstadoEnvio.EN_CAMINO).pedido(pedido).build();

        when(envioRepository.findById(1L)).thenReturn(Optional.of(envio));

        envioService.eliminar(1L);

        assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.PAGADO);
        assertThat(pedido.getEnvio()).isNull();
        verify(pedidoRepository).save(pedido);
        verify(envioRepository).delete(envio);
    }

    @Test
    void eliminar_envioInexistente_lanzaNotFound() {
        when(envioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> envioService.eliminar(99L))
                .isInstanceOf(EntidadNoEncontradaException.class);
    }
}
