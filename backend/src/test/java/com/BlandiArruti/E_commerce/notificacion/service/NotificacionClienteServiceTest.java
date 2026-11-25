package com.BlandiArruti.E_commerce.notificacion.service;

import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.enums.EstadoEnvio;
import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.enums.TipoNotificacion;
import com.BlandiArruti.E_commerce.envio.entity.Envio;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.notificacion.entity.Notificacion;
import com.BlandiArruti.E_commerce.notificacion.repository.NotificacionRepository;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacionClienteServiceTest {

    @Mock NotificacionRepository notificacionRepository;

    @InjectMocks
    NotificacionClienteService service;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private static Cliente cliente(Long id) {
        return Cliente.builder().id(id).email("cliente@test.com").contrasenia("hash").activo(true).build();
    }

    private void autenticarComoCliente(Long idCliente) {
        UsuarioDetails details = UsuarioDetails.fromCliente(cliente(idCliente));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    private Notificacion guardada() {
        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        return captor.getValue();
    }

    // ─── avisos de pedidos ───────────────────────────────────────────────────

    @Test
    void avisarPedido_pagado_guardaNotificacionDelDuenio() {
        Pedido pedido = Pedido.builder().id(7L).cliente(cliente(3L)).estadoPedido(EstadoPedido.PAGADO).build();

        service.avisarPedido(pedido);

        Notificacion n = guardada();
        assertThat(n.getCliente().getId()).isEqualTo(3L);
        assertThat(n.getTipo()).isEqualTo(TipoNotificacion.PEDIDO);
        assertThat(n.getIdReferencia()).isEqualTo(7L);
        assertThat(n.getTitulo()).isEqualTo("Pago acreditado");
        assertThat(n.isLeida()).isFalse();
        assertThat(n.getFechaCreacion()).isNotNull();
    }

    @Test
    void avisarEnvio_despachado_incluyeEmpresaYSeguimiento() {
        Pedido pedido = Pedido.builder().id(7L).cliente(cliente(3L)).estadoPedido(EstadoPedido.DESPACHADO).build();
        Envio envio = Envio.builder().pedido(pedido).estado(EstadoEnvio.DESPACHADO)
                .empresa("Andreani").numeroSeguimiento("AB123").build();

        service.avisarEnvio(envio);

        Notificacion n = guardada();
        assertThat(n.getTitulo()).isEqualTo("Pedido despachado");
        assertThat(n.getMensaje()).contains("Andreani").contains("AB123");
    }

    @Test
    void avisarEnvio_enCamino_aunqueElPedidoSigaDespachado() {
        Pedido pedido = Pedido.builder().id(7L).cliente(cliente(3L)).estadoPedido(EstadoPedido.DESPACHADO).build();
        Envio envio = Envio.builder().pedido(pedido).estado(EstadoEnvio.EN_CAMINO).empresa("Andreani").build();

        service.avisarEnvio(envio);

        assertThat(guardada().getTitulo()).isEqualTo("Pedido en camino");
    }

    @Test
    void avisarPedido_pendienteDePago_noAvisa() {
        Pedido pedido = Pedido.builder().id(7L).cliente(cliente(3L)).estadoPedido(EstadoPedido.PENDIENTE_PAGO).build();

        service.avisarPedido(pedido);

        verifyNoInteractions(notificacionRepository);
    }

    // ─── ofertas ─────────────────────────────────────────────────────────────

    @Test
    void avisarOferta_insertaParaTodosLosClientesActivos() {
        Producto producto = Producto.builder().id(5L).nombre("Whey Protein").descuento(20).build();

        service.avisarOferta(producto);

        verify(notificacionRepository).crearParaClientesActivos(
                eq("OFERTA"), contains("20% OFF"), contains("Whey Protein"), eq(5L), any());
    }

    // ─── lo que ve el cliente ────────────────────────────────────────────────

    @Test
    void contarNoLeidas_deOtroCliente_lanzaAccessDenied() {
        autenticarComoCliente(1L);

        assertThatThrownBy(() -> service.contarNoLeidas(2L)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(notificacionRepository);
    }

    @Test
    void marcarLeida_marcaLaDelCliente() {
        autenticarComoCliente(1L);
        Notificacion n = Notificacion.builder().id(9L).cliente(cliente(1L)).tipo(TipoNotificacion.PEDIDO)
                .titulo("t").mensaje("m").build();
        when(notificacionRepository.findByIdAndClienteId(9L, 1L)).thenReturn(Optional.of(n));

        assertThat(service.marcarLeida(1L, 9L).leida()).isTrue();
        assertThat(n.isLeida()).isTrue();
    }

    @Test
    void marcarLeida_queNoEsDelCliente_lanza404() {
        autenticarComoCliente(1L);
        when(notificacionRepository.findByIdAndClienteId(9L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcarLeida(1L, 9L)).isInstanceOf(EntidadNoEncontradaException.class);
    }
}
