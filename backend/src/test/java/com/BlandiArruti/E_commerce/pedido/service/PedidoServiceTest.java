package com.BlandiArruti.E_commerce.pedido.service;

import com.BlandiArruti.E_commerce.administrador.entity.Administrador;
import com.BlandiArruti.E_commerce.cupon.entity.Cupon;
import com.BlandiArruti.E_commerce.cupon.service.ICuponService;
import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.carrito.entity.Carrito;
import com.BlandiArruti.E_commerce.carrito.entity.ItemCarrito;
import com.BlandiArruti.E_commerce.carrito.repository.CarritoRepository;
import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.entity.Direccion;
import com.BlandiArruti.E_commerce.cliente.repository.ClienteRepository;
import com.BlandiArruti.E_commerce.cliente.repository.DireccionRepository;
import com.BlandiArruti.E_commerce.cotizacion.dto.request.OpcionEnvioRequest;
import com.BlandiArruti.E_commerce.cotizacion.service.CotizacionEnvioService;
import com.BlandiArruti.E_commerce.envio.entity.Envio;
import com.BlandiArruti.E_commerce.envio.mapper.EnvioMapper;
import com.BlandiArruti.E_commerce.envio.repository.EnvioRepository;
import com.BlandiArruti.E_commerce.enums.EstadoEnvio;
import com.BlandiArruti.E_commerce.enums.ConfirmacionRecepcion;
import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.enums.TipoFactura;
import com.BlandiArruti.E_commerce.enums.TipoReembolso;
import com.BlandiArruti.E_commerce.enums.EstadoReembolso;
import com.BlandiArruti.E_commerce.pedido.dto.request.ResolucionReembolsoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.request.SolicitudReembolsoRequest;
import java.time.LocalDateTime;
import com.BlandiArruti.E_commerce.exception.ServicioExternoException;
import com.BlandiArruti.E_commerce.pedido.dto.response.CancelacionPedidoResponse;
import com.BlandiArruti.E_commerce.exception.ConflictoException;
import com.BlandiArruti.E_commerce.factura.dto.response.FacturaPdfResponse;
import com.BlandiArruti.E_commerce.factura.pdf.FacturaPdfGenerator;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.exception.StockInsuficienteException;
import com.BlandiArruti.E_commerce.exception.PedidoNoModificableException;
import com.BlandiArruti.E_commerce.factura.entity.Factura;
import com.BlandiArruti.E_commerce.factura.mapper.FacturaMapper;
import com.BlandiArruti.E_commerce.factura.repository.FacturaRepository;
import com.BlandiArruti.E_commerce.notificacion.service.NotificacionClienteService;
import com.BlandiArruti.E_commerce.notificacion.service.NotificacionService;
import com.BlandiArruti.E_commerce.pedido.dto.request.PagoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.request.ConfirmarRecepcionRequest;
import com.BlandiArruti.E_commerce.pedido.dto.request.EstadoPedidoRequest;
import com.BlandiArruti.E_commerce.envio.dto.request.EnvioRequest;
import com.BlandiArruti.E_commerce.pedido.entity.ItemPedido;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import org.mockito.ArgumentCaptor;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import com.BlandiArruti.E_commerce.pedido.dto.request.ItemPedidoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.request.PedidoRequest;
import com.BlandiArruti.E_commerce.pedido.mapper.PedidoMapper;
import com.BlandiArruti.E_commerce.pedido.repository.PedidoRepository;
import com.BlandiArruti.E_commerce.producto.entity.Variante;
import com.BlandiArruti.E_commerce.producto.repository.ProductoRepository;
import com.BlandiArruti.E_commerce.producto.repository.VarianteRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock PedidoRepository pedidoRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock ProductoRepository productoRepository;
    @Mock VarianteRepository varianteRepository;
    @Mock FacturaRepository facturaRepository;
    @Mock EnvioRepository envioRepository;
    @Mock DireccionRepository direccionRepository;
    @Mock NotificacionService notificacionService;
    @Mock PedidoMapper pedidoMapper;
    @Mock FacturaMapper facturaMapper;
    @Mock EnvioMapper envioMapper;
    @Mock CotizacionEnvioService cotizacionEnvioService;
    @Mock ICuponService cuponService;
    @Mock CarritoRepository carritoRepository;
    @Mock FacturaPdfGenerator facturaPdfGenerator;
    @Mock NotificacionClienteService notificacionClienteService;
    @Mock IReembolsoPagos reembolsoPagos;

    @InjectMocks
    PedidoService pedidoService;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    // ─── cancelar() ──────────────────────────────────────────────────────────

    @Test
    void cancelar_desdePendientePago_noDevuelveStockYCancela() {
        Variante variante = Variante.builder().id(1L).stock(10).build();
        ItemPedido item = ItemPedido.builder().variante(variante).cantidad(3).build();
        Pedido pedido = pedidoConEstado(EstadoPedido.PENDIENTE_PAGO, item);

        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        pedidoService.cancelar(1L);

        assertThat(variante.getStock()).isEqualTo(10);
        verify(varianteRepository, never()).save(any());
        assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.CANCELADO);
    }

    @Test
    void cancelar_desdeEnPreparacion_noDevuelveStock() {
        Variante variante = Variante.builder().id(1L).stock(10).build();
        ItemPedido item = ItemPedido.builder().variante(variante).cantidad(3).build();
        Pedido pedido = pedidoConEstado(EstadoPedido.EN_PREPARACION, item);

        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        pedidoService.cancelar(1L);

        assertThat(variante.getStock()).isEqualTo(10);
        verify(varianteRepository, never()).save(any());
    }

    @Test
    void cancelar_desdePagado_devuelveStock() {
        Variante variante = Variante.builder().id(1L).stock(7).build();
        ItemPedido item = ItemPedido.builder().variante(variante).cantidad(3).build();
        Pedido pedido = pedidoConEstado(EstadoPedido.PAGADO, item);

        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(reembolsoPagos.reembolsarCobros(pedido)).thenReturn(1500.0);

        pedidoService.cancelar(1L);

        assertThat(variante.getStock()).isEqualTo(10);
        verify(varianteRepository).save(variante);
    }

    @Test
    void cancelar_desdeDespachado_devuelveStock() {
        Variante variante = Variante.builder().id(1L).stock(5).build();
        ItemPedido item = ItemPedido.builder().variante(variante).cantidad(2).build();
        Pedido pedido = pedidoConEstado(EstadoPedido.DESPACHADO, item);

        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(reembolsoPagos.reembolsarCobros(pedido)).thenReturn(1500.0);

        pedidoService.cancelar(1L);

        assertThat(variante.getStock()).isEqualTo(7);
    }

    @Test
    void cancelar_clienteDesdePendientePago_cancelaSinReembolso() {
        Pedido pedido = pedidoConEstado(EstadoPedido.PENDIENTE_PAGO);

        autenticarComoCliente(1L);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        CancelacionPedidoResponse respuesta = pedidoService.cancelar(1L);

        assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.CANCELADO);
        assertThat(respuesta.reembolso()).isEqualTo(TipoReembolso.NO_CORRESPONDE);
        verify(reembolsoPagos, never()).reembolsarCobros(any());
    }

    // ─── RF04: el cliente cancela un pedido pagado y no despachado ───────────

    @Test
    void cancelar_clienteDesdePagado_devuelveLaPlataElStockYCancela() {
        Variante variante = variante(7);
        Pedido pedido = pedidoConEstado(EstadoPedido.PAGADO, itemDe(1500.0, variante, 3));

        autenticarComoCliente(1L);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(reembolsoPagos.reembolsarCobros(pedido)).thenReturn(1500.0);

        CancelacionPedidoResponse respuesta = pedidoService.cancelar(1L);

        assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.CANCELADO);
        assertThat(variante.getStock()).isEqualTo(10);
        assertThat(respuesta.reembolso()).isEqualTo(TipoReembolso.AUTOMATICO);
        assertThat(respuesta.montoReembolsado()).isEqualTo(1500.0);
        assertThat(respuesta.mensaje()).contains("Mercado Pago");
        // Lo canceló el propio cliente: no se le manda aviso.
        verify(notificacionClienteService, never()).avisarPedido(any());
    }

    @Test
    void cancelar_clienteDesdeDespachado_lanzaExcepcionYNoDevuelveNada() {
        Variante variante = variante(7);
        Pedido pedido = pedidoConEstado(EstadoPedido.DESPACHADO, itemDe(1500.0, variante, 3));

        autenticarComoCliente(1L);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.cancelar(1L))
                .isInstanceOf(PedidoNoModificableException.class)
                .hasMessageContaining("despachado");
        assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.DESPACHADO);
        assertThat(variante.getStock()).isEqualTo(7);
        verify(reembolsoPagos, never()).reembolsarCobros(any());
    }

    @Test
    void cancelar_siMercadoPagoRechazaLaDevolucion_elPedidoQuedaComoEstaba() {
        Variante variante = variante(7);
        Pedido pedido = pedidoConEstado(EstadoPedido.PAGADO, itemDe(1500.0, variante, 3));

        autenticarComoCliente(1L);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(reembolsoPagos.reembolsarCobros(pedido))
                .thenThrow(new ServicioExternoException("Mercado Pago rechazó la devolución"));

        assertThatThrownBy(() -> pedidoService.cancelar(1L)).isInstanceOf(ServicioExternoException.class);
        assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.PAGADO);
        assertThat(variante.getStock()).isEqualTo(7);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void cancelar_pagadoSinPasarela_cancelaYAvisaQueLaDevolucionEsManual() {
        Pedido pedido = pedidoConEstado(EstadoPedido.PAGADO, itemDe(1500.0, variante(7), 3));

        autenticarComoCliente(1L);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(reembolsoPagos.reembolsarCobros(pedido)).thenReturn(0.0);

        CancelacionPedidoResponse respuesta = pedidoService.cancelar(1L);

        assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.CANCELADO);
        assertThat(respuesta.reembolso()).isEqualTo(TipoReembolso.MANUAL);
        assertThat(respuesta.montoReembolsado()).isEqualTo(1500.0);
    }

    @Test
    void cancelar_desdeEntregado_lanzaExcepcion() {
        Pedido pedido = pedidoConEstado(EstadoPedido.ENTREGADO);

        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.cancelar(1L))
                .isInstanceOf(PedidoNoModificableException.class);
    }

    @Test
    void cancelar_desdeCancelado_lanzaExcepcion() {
        Pedido pedido = pedidoConEstado(EstadoPedido.CANCELADO);

        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.cancelar(1L))
                .isInstanceOf(PedidoNoModificableException.class);
    }

    // ─── RF05: reembolso de un pedido entregado ──────────────────────────────

    private Pedido entregadoHace(int dias) {
        Pedido pedido = pedidoConEstado(EstadoPedido.ENTREGADO, itemDe(1500.0, variante(7), 3));
        pedido.setFechaEntrega(LocalDateTime.now().minusDays(dias));
        return pedido;
    }

    @Test
    void solicitarReembolso_dentroDelPlazo_quedaSolicitadoConElMotivo() {
        Pedido pedido = entregadoHace(3);
        autenticarComoCliente(1L);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        pedidoService.solicitarReembolso(1L, new SolicitudReembolsoRequest("  El talle no me quedó bien  "));

        assertThat(pedido.getEstadoReembolso()).isEqualTo(EstadoReembolso.SOLICITADO);
        assertThat(pedido.getMotivoReembolso()).isEqualTo("El talle no me quedó bien");
        assertThat(pedido.getFechaSolicitudReembolso()).isNotNull();
        // Todavía no se devuelve nada: lo decide el admin.
        verify(reembolsoPagos, never()).reembolsarCobros(any());
    }

    @Test
    void solicitarReembolso_pasadosLos10Dias_rechaza() {
        Pedido pedido = entregadoHace(11);
        autenticarComoCliente(1L);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.solicitarReembolso(1L, new SolicitudReembolsoRequest("Me arrepentí de la compra")))
                .isInstanceOf(PedidoNoModificableException.class)
                .hasMessageContaining("10 días");
        assertThat(pedido.getEstadoReembolso()).isNull();
    }

    @Test
    void solicitarReembolso_pedidoNoEntregado_rechaza() {
        Pedido pedido = pedidoConEstado(EstadoPedido.DESPACHADO);
        autenticarComoCliente(1L);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.solicitarReembolso(1L, new SolicitudReembolsoRequest("Me arrepentí de la compra")))
                .isInstanceOf(PedidoNoModificableException.class);
    }

    @Test
    void solicitarReembolso_yaPedido_esConflicto() {
        Pedido pedido = entregadoHace(2);
        pedido.setEstadoReembolso(EstadoReembolso.SOLICITADO);
        autenticarComoCliente(1L);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.solicitarReembolso(1L, new SolicitudReembolsoRequest("Me arrepentí de la compra")))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void solicitarReembolso_deOtroCliente_esAccesoDenegado() {
        Pedido pedido = entregadoHace(2);
        autenticarComoCliente(2L);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.solicitarReembolso(1L, new SolicitudReembolsoRequest("Me arrepentí de la compra")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void solicitarReembolso_pedidoViejoSinFechaDeEntrega_usaLaLlegadaDelEnvio() {
        Pedido pedido = pedidoConEstado(EstadoPedido.ENTREGADO);
        pedido.setEnvio(Envio.builder().fechaLlegada(LocalDate.now().minusDays(2)).build());
        autenticarComoCliente(1L);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        pedidoService.solicitarReembolso(1L, new SolicitudReembolsoRequest("Me arrepentí de la compra"));

        assertThat(pedido.getEstadoReembolso()).isEqualTo(EstadoReembolso.SOLICITADO);
    }

    @Test
    void resolverReembolso_aprobado_devuelveEnMercadoPagoSinTocarElStockYAvisa() {
        Variante variante = variante(7);
        Pedido pedido = pedidoConEstado(EstadoPedido.ENTREGADO, itemDe(1500.0, variante, 3));
        pedido.setEstadoReembolso(EstadoReembolso.SOLICITADO);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reembolsoPagos.reembolsarCobros(pedido)).thenReturn(1500.0);

        pedidoService.resolverReembolso(1L, new ResolucionReembolsoRequest(true, null));

        assertThat(pedido.getEstadoReembolso()).isEqualTo(EstadoReembolso.APROBADO);
        assertThat(pedido.getTipoReembolso()).isEqualTo(TipoReembolso.AUTOMATICO);
        assertThat(pedido.getMontoReembolsado()).isEqualTo(1500.0);
        assertThat(pedido.getFechaResolucionReembolso()).isNotNull();
        assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.ENTREGADO);
        assertThat(variante.getStock()).isEqualTo(7);
        verify(notificacionClienteService).avisarReembolso(pedido);
    }

    @Test
    void resolverReembolso_siMercadoPagoFalla_quedaSolicitado() {
        Pedido pedido = pedidoConEstado(EstadoPedido.ENTREGADO, itemDe(1500.0, variante(7), 3));
        pedido.setEstadoReembolso(EstadoReembolso.SOLICITADO);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(reembolsoPagos.reembolsarCobros(pedido)).thenThrow(new ServicioExternoException("Mercado Pago rechazó la devolución"));

        assertThatThrownBy(() -> pedidoService.resolverReembolso(1L, new ResolucionReembolsoRequest(true, null)))
                .isInstanceOf(ServicioExternoException.class);
        assertThat(pedido.getEstadoReembolso()).isEqualTo(EstadoReembolso.SOLICITADO);
        verify(notificacionClienteService, never()).avisarReembolso(any());
    }

    @Test
    void resolverReembolso_rechazadoSinMotivo_pideElMotivo() {
        Pedido pedido = pedidoConEstado(EstadoPedido.ENTREGADO);
        pedido.setEstadoReembolso(EstadoReembolso.SOLICITADO);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.resolverReembolso(1L, new ResolucionReembolsoRequest(false, "  ")))
                .isInstanceOf(EcommerceException.class);
        assertThat(pedido.getEstadoReembolso()).isEqualTo(EstadoReembolso.SOLICITADO);
    }

    @Test
    void resolverReembolso_rechazadoConMotivo_guardaLaRespuestaYAvisa() {
        Pedido pedido = pedidoConEstado(EstadoPedido.ENTREGADO);
        pedido.setEstadoReembolso(EstadoReembolso.SOLICITADO);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        pedidoService.resolverReembolso(1L, new ResolucionReembolsoRequest(false, "El producto se abrió y se usó."));

        assertThat(pedido.getEstadoReembolso()).isEqualTo(EstadoReembolso.RECHAZADO);
        assertThat(pedido.getRespuestaReembolso()).isEqualTo("El producto se abrió y se usó.");
        verify(reembolsoPagos, never()).reembolsarCobros(any());
        verify(notificacionClienteService).avisarReembolso(pedido);
    }

    @Test
    void resolverReembolso_sinSolicitud_esConflicto() {
        Pedido pedido = pedidoConEstado(EstadoPedido.ENTREGADO);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.resolverReembolso(1L, new ResolucionReembolsoRequest(true, null)))
                .isInstanceOf(ConflictoException.class);
    }

    // ─── cambiarEstado() ─────────────────────────────────────────────────────

    @Test
    void cambiarEstado_despachadoACancelado_devuelveStock() {
        Variante variante = Variante.builder().id(1L).stock(4).build();
        ItemPedido item = ItemPedido.builder().variante(variante).cantidad(2).build();
        Pedido pedido = pedidoConEstado(EstadoPedido.DESPACHADO, item);

        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoMapper.toResponse(any())).thenReturn(null);

        pedidoService.cambiarEstado(1L, new EstadoPedidoRequest(EstadoPedido.CANCELADO));

        assertThat(variante.getStock()).isEqualTo(6);
        verify(varianteRepository).save(variante);
    }

    @Test
    void cambiarEstado_pendienteACancelado_noDevuelveStock() {
        Variante variante = Variante.builder().id(1L).stock(10).build();
        ItemPedido item = ItemPedido.builder().variante(variante).cantidad(3).build();
        Pedido pedido = pedidoConEstado(EstadoPedido.PENDIENTE_PAGO, item);

        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoMapper.toResponse(any())).thenReturn(null);

        pedidoService.cambiarEstado(1L, new EstadoPedidoRequest(EstadoPedido.CANCELADO));

        assertThat(variante.getStock()).isEqualTo(10);
        verify(varianteRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_pagadoACancelado_devuelveStock() {
        Variante variante = Variante.builder().id(1L).stock(4).build();
        ItemPedido item = ItemPedido.builder().variante(variante).cantidad(2).build();
        Pedido pedido = pedidoConEstado(EstadoPedido.PAGADO, item);

        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoMapper.toResponse(any())).thenReturn(null);

        pedidoService.cambiarEstado(1L, new EstadoPedidoRequest(EstadoPedido.CANCELADO));

        assertThat(variante.getStock()).isEqualTo(6);
        verify(varianteRepository).save(variante);
    }

    @Test
    void cambiarEstado_transicionInvalida_lanzaExcepcion() {
        Pedido pedido = pedidoConEstado(EstadoPedido.PENDIENTE_PAGO);

        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.cambiarEstado(1L, new EstadoPedidoRequest(EstadoPedido.ENTREGADO)))
                .isInstanceOf(EcommerceException.class);
    }

    @Test
    void cambiarEstado_aPagadoManualmente_noSePermite() {
        Variante variante = Variante.builder().id(1L).stock(10).build();
        ItemPedido item = ItemPedido.builder().variante(variante).cantidad(3).build();
        Pedido pedido = pedidoConEstado(EstadoPedido.EN_PREPARACION, item);

        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.cambiarEstado(1L, new EstadoPedidoRequest(EstadoPedido.PAGADO)))
                .isInstanceOf(EcommerceException.class)
                .hasMessageContaining("pagar");

        // El pedido no se movió y no se tocó ni el stock ni la facturación.
        assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.EN_PREPARACION);
        assertThat(variante.getStock()).isEqualTo(10);
        verify(facturaRepository, never()).save(any());
    }

    // ─── pagar() ─────────────────────────────────────────────────────────────

    @Test
    void pagar_desdeEnPreparacion_descuentaStockYFactura() {
        Variante variante = Variante.builder().id(1L).stock(10).build();
        ItemPedido item = ItemPedido.builder()
                .variante(variante).cantidad(3).precioProducto(300.0).build();
        Pedido pedido = pedidoConEstado(EstadoPedido.EN_PREPARACION, item);

        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(pedido)).thenReturn(pedido);
        when(pedidoMapper.toResponse(any())).thenReturn(null);

        pedidoService.pagar(1L, new PagoRequest(TipoFactura.B));

        assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.PAGADO);
        assertThat(variante.getStock()).isEqualTo(7);
        verify(facturaRepository).save(any());
    }

    @Test
    void pagar_sacaDelCarritoSoloLoComprado() {
        // El carrito ya no se vacía al confirmar: lo comprado sale recién al cobrar.
        Producto proteina = Producto.builder().id(2L).build();
        Producto creatina = Producto.builder().id(3L).build();
        Variante chocolate = Variante.builder().id(1L).producto(proteina).stock(10).build();
        ItemPedido comprado = ItemPedido.builder()
                .producto(proteina).variante(chocolate).cantidad(2).precioProducto(200.0).build();
        Pedido pedido = pedidoConEstado(EstadoPedido.PENDIENTE_PAGO, comprado);

        Carrito carrito = Carrito.builder().id(1L).build();
        ItemCarrito mismoQueElPedido = ItemCarrito.builder().carrito(carrito)
                .producto(proteina).variante(chocolate).cantidad(2).build();
        // Lo agregó después de confirmar el pedido: tiene que quedar.
        ItemCarrito agregadoDespues = ItemCarrito.builder().carrito(carrito)
                .producto(creatina).cantidad(1).build();
        carrito.getItems().add(mismoQueElPedido);
        carrito.getItems().add(agregadoDespues);

        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(pedido)).thenReturn(pedido);
        when(carritoRepository.findByClienteId(1L)).thenReturn(Optional.of(carrito));

        pedidoService.pagar(1L, new PagoRequest(TipoFactura.B));

        assertThat(carrito.getItems()).containsExactly(agregadoDespues);
        verify(carritoRepository).save(carrito);
    }

    @Test
    void cancelarPendientesSinIntentoDePago_losCancelaYDevuelveElCupon() {
        Cupon cupon = Cupon.builder().id(4L).codigo("BIENVENIDA").build();
        Pedido abandonado = pedidoConEstado(EstadoPedido.PENDIENTE_PAGO);
        abandonado.setCupon(cupon);
        when(pedidoRepository.findPendientesSinIntentoDePago(1L)).thenReturn(List.of(abandonado));

        pedidoService.cancelarPendientesSinIntentoDePago(1L);

        assertThat(abandonado.getEstadoPedido()).isEqualTo(EstadoPedido.CANCELADO);
        verify(cuponService).devolverUso(cupon);
        verify(pedidoRepository).save(abandonado);
    }

    @Test
    void pagar_usaElStockLeidoConBloqueo_noElQueQuedoEnMemoria() {
        // Otro pago ya se llevó unidades: el stock real (bloqueado) es menor al que tenía el pedido en memoria.
        Variante enMemoria = Variante.builder().id(1L).stock(10).build();
        Variante real = Variante.builder().id(1L).stock(2).build();
        ItemPedido item = ItemPedido.builder().variante(enMemoria).cantidad(3).precioProducto(300.0).build();
        Pedido pedido = pedidoConEstado(EstadoPedido.EN_PREPARACION, item);

        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(varianteRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(real));

        assertThatThrownBy(() -> pedidoService.pagar(1L, new PagoRequest(TipoFactura.B)))
                .isInstanceOf(StockInsuficienteException.class);
        verify(facturaRepository, never()).save(any());
    }

    @Test
    void pagar_pedidoYaPagado_lanzaNoModificable() {
        Pedido pedido = pedidoConEstado(EstadoPedido.PAGADO);

        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.pagar(1L, new PagoRequest(TipoFactura.B)))
                .isInstanceOf(PedidoNoModificableException.class);
        verify(facturaRepository, never()).save(any());
    }

    @Test
    void pagar_conCostoEnvio_facturaIncluyeEnvio() {
        Variante variante = Variante.builder().id(1L).stock(10).build();
        ItemPedido item = ItemPedido.builder()
                .variante(variante).cantidad(2).precioProducto(500.0).build();
        Pedido pedido = pedidoConEstado(EstadoPedido.EN_PREPARACION, item);
        pedido.setCostoEnvio(1200.0);

        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(pedido)).thenReturn(pedido);
        when(pedidoMapper.toResponse(any())).thenReturn(null);

        pedidoService.pagar(1L, new PagoRequest(TipoFactura.B));

        ArgumentCaptor<Factura> captor = ArgumentCaptor.forClass(Factura.class);
        verify(facturaRepository).save(captor.capture());
        assertThat(captor.getValue().getPrecioTotal()).isEqualTo(1700.0);
    }

    // ─── crearEnvio() ────────────────────────────────────────────────────────

    @Test
    void crearEnvio_cuandoYaExisteEnvio_lanzaConflicto() {
        Pedido pedido = pedidoConEstado(EstadoPedido.PAGADO);

        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(envioRepository.findByPedidoId(1L)).thenReturn(Optional.of(Envio.builder().build()));

        var request = new EnvioRequest(" Andreani ", "360002541870", LocalDate.now(), LocalDate.now().plusDays(3));

        assertThatThrownBy(() -> pedidoService.crearEnvio(1L, request))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void crearEnvio_pedidoNoPagado_lanzaExcepcion() {
        Pedido pedido = pedidoConEstado(EstadoPedido.PENDIENTE_PAGO);

        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        var request = new EnvioRequest(" Andreani ", "360002541870", LocalDate.now(), LocalDate.now().plusDays(3));

        assertThatThrownBy(() -> pedidoService.crearEnvio(1L, request))
                .isInstanceOf(EcommerceException.class);
    }

    @Test
    void crearEnvio_exitoso_enviaNotificacionYActualizaEstado() {
        Direccion direccion = Direccion.builder().id(1L).build();
        Cliente cliente = Cliente.builder().id(1L).build();
        Pedido pedido = Pedido.builder().id(1L).cliente(cliente).estadoPedido(EstadoPedido.PAGADO).direccionEnvio(direccion).build();

        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(envioRepository.findByPedidoId(1L)).thenReturn(Optional.empty());
        when(envioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(envioMapper.toResponse(any())).thenReturn(null);

        pedidoService.crearEnvio(1L, new EnvioRequest(" Andreani ", "360002541870", LocalDate.now(), LocalDate.now().plusDays(3)));

        assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.DESPACHADO);
        verify(notificacionService).notificarEnvioActualizado(any());
    }

    // ─── Lo que usa PagoService ───────────────────────────────────────────────

    @Test
    void registrarPagoAprobado_porElTotal_pagaDescuentaStockYEmiteFactura() {
        Variante variante = variante(10);
        Pedido pedido = pedidoConEstado(EstadoPedido.PENDIENTE_PAGO, itemDe(500.0, variante, 2));
        when(pedidoRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(pedido));
        when(facturaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean pagado = pedidoService.registrarPagoAprobado(1L, TipoFactura.B, new BigDecimal("500.00"));

        assertThat(pagado).isTrue();
        assertThat(variante.getStock()).isEqualTo(8);
        assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.PAGADO);
        assertThat(pedido.getFactura().getTipoFactura()).isEqualTo(TipoFactura.B);
        verify(notificacionService).notificarFacturaGenerada(any());
    }

    @Test
    void registrarPagoAprobado_pedidoYaPagado_noCobraDosVeces() {
        Pedido pedido = pedidoConEstado(EstadoPedido.PAGADO, itemDe(500.0, variante(10), 2));
        when(pedidoRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(pedido));

        boolean pagado = pedidoService.registrarPagoAprobado(1L, TipoFactura.B, new BigDecimal("500.00"));

        assertThat(pagado).isTrue();
        verify(varianteRepository, never()).save(any());
        verify(facturaRepository, never()).save(any());
    }

    @Test
    void registrarPagoAprobado_montoMenorAlTotal_noAcredita() {
        Pedido pedido = pedidoConEstado(EstadoPedido.PENDIENTE_PAGO, itemDe(500.0, variante(10), 2));
        when(pedidoRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(pedido));

        boolean pagado = pedidoService.registrarPagoAprobado(1L, TipoFactura.B, new BigDecimal("100.00"));

        assertThat(pagado).isFalse();
        assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.PENDIENTE_PAGO);
        verify(facturaRepository, never()).save(any());
    }

    @Test
    void registrarPagoAprobado_pedidoCancelado_noAcredita() {
        Pedido pedido = pedidoConEstado(EstadoPedido.CANCELADO, itemDe(500.0, variante(10), 2));
        when(pedidoRepository.findByIdParaActualizar(1L)).thenReturn(Optional.of(pedido));

        assertThat(pedidoService.registrarPagoAprobado(1L, TipoFactura.B, new BigDecimal("500.00"))).isFalse();
        verify(facturaRepository, never()).save(any());
    }

    @Test
    void obtenerParaPagar_pedidoDeOtroCliente_rechaza() {
        Pedido pedido = pedidoConEstado(EstadoPedido.PENDIENTE_PAGO, itemDe(500.0, variante(10), 2));
        autenticarComoCliente(2L);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.obtenerParaPagar(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void obtenerParaPagar_pedidoYaPagado_rechaza() {
        Pedido pedido = pedidoConEstado(EstadoPedido.PAGADO, itemDe(500.0, variante(10), 2));
        autenticarComoCliente(1L);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.obtenerParaPagar(1L))
                .isInstanceOf(PedidoNoModificableException.class);
    }

    // ─── crear: precio por variante (SCRUM-154) ───────────────────────────────

    private final Cliente clienteCrear = Cliente.builder().id(1L).email("c@test.com").dni("30000000").contrasenia("x").activo(true).build();

    private Producto proteina() {
        return Producto.builder().id(2L).nombre("Whey").precio(30000.0).descuento(0).activo(true).build();
    }

    @Test
    void crear_varianteConPrecioPropio_guardaElPrecioDeLaVariante() {
        Producto producto = proteina();
        Variante dosKilos = Variante.builder().id(7L).producto(producto).stock(10).precio(55000.0).build();
        when(clienteRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(clienteCrear));
        when(productoRepository.findById(2L)).thenReturn(Optional.of(producto));
        when(varianteRepository.findById(7L)).thenReturn(Optional.of(dosKilos));
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        pedidoService.crear(new PedidoRequest(null, null, null, List.of(new ItemPedidoRequest(2L, 7L, 2)), null),
                UsuarioDetails.fromCliente(clienteCrear));

        ArgumentCaptor<Pedido> guardado = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(guardado.capture());
        assertThat(guardado.getValue().getItems()).singleElement()
                .extracting(ItemPedido::getPrecioProducto).isEqualTo(110000.0);
    }

    @Test
    void crear_varianteDeOtroProducto_rechaza() {
        Producto producto = proteina();
        Producto otro = Producto.builder().id(9L).precio(90000.0).build();
        Variante deOtro = Variante.builder().id(7L).producto(otro).stock(10).precio(90000.0).build();
        when(clienteRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(clienteCrear));
        when(productoRepository.findById(2L)).thenReturn(Optional.of(producto));
        when(varianteRepository.findById(7L)).thenReturn(Optional.of(deOtro));

        assertThatThrownBy(() -> pedidoService.crear(
                new PedidoRequest(null, null, null, List.of(new ItemPedidoRequest(2L, 7L, 1)), null),
                UsuarioDetails.fromCliente(clienteCrear)))
                .isInstanceOf(EcommerceException.class)
                .hasMessageContaining("no pertenece al producto");
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void crear_clienteSinDni_pideCompletarloAntesDeComprar() {
        // Cuenta creada con Google o GitHub: todavía no cargó el DNI, que va en la factura.
        Cliente sinDni = Cliente.builder().id(1L).email("c@gmail.com").contrasenia("x").activo(true).build();
        when(clienteRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(sinDni));

        assertThatThrownBy(() -> pedidoService.crear(
                new PedidoRequest(null, null, null, List.of(new ItemPedidoRequest(2L, null, 1)), null),
                UsuarioDetails.fromCliente(sinDni)))
                .isInstanceOf(EcommerceException.class)
                .hasMessageContaining("Completá tu DNI");
        verify(productoRepository, never()).findById(any());
        verify(pedidoRepository, never()).save(any());
    }

    // ─── crear: huecos encontrados en la auditoría ────────────────────────────

    @Test
    void crear_sinVarianteDeUnProductoConVariantes_rechaza() {
        Producto producto = proteina();
        when(clienteRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(clienteCrear));
        when(productoRepository.findById(2L)).thenReturn(Optional.of(producto));
        when(varianteRepository.findByProductoIdAndActivoTrue(2L))
                .thenReturn(List.of(Variante.builder().id(7L).producto(producto).stock(10).precio(80000.0).build()));

        assertThatThrownBy(() -> pedidoService.crear(
                new PedidoRequest(null, null, null, List.of(new ItemPedidoRequest(2L, null, 1)), null),
                UsuarioDetails.fromCliente(clienteCrear)))
                .isInstanceOf(EcommerceException.class)
                .hasMessageContaining("tiene variantes");
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void crear_productoSinVariantes_seCompraSinIndicarVariante() {
        Producto producto = proteina();
        when(clienteRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(clienteCrear));
        when(productoRepository.findById(2L)).thenReturn(Optional.of(producto));
        when(varianteRepository.findByProductoIdAndActivoTrue(2L)).thenReturn(List.of());
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        pedidoService.crear(new PedidoRequest(null, null, null, List.of(new ItemPedidoRequest(2L, null, 2)), null),
                UsuarioDetails.fromCliente(clienteCrear));

        verify(pedidoRepository).save(any());
    }

    @Test
    void crear_productoDadoDeBaja_rechaza() {
        Producto producto = proteina();
        producto.setActivo(false);
        when(clienteRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(clienteCrear));
        when(productoRepository.findById(2L)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> pedidoService.crear(
                new PedidoRequest(null, null, null, List.of(new ItemPedidoRequest(2L, null, 1)), null),
                UsuarioDetails.fromCliente(clienteCrear)))
                .isInstanceOf(EcommerceException.class)
                .hasMessageContaining("ya no está a la venta");
        verify(pedidoRepository, never()).save(any());
    }

    // ─── crear: cupón de descuento ────────────────────────────────────────────

    @Test
    void crear_conCupon_loAplicaSobreLosProductosYLoGuardaEnElPedido() {
        Producto producto = proteina();
        Variante variante = Variante.builder().id(7L).producto(producto).stock(10).build();
        Cupon cupon = Cupon.builder().id(3L).codigo("VERANO10").build();
        when(clienteRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(clienteCrear));
        when(productoRepository.findById(2L)).thenReturn(Optional.of(producto));
        when(varianteRepository.findById(7L)).thenReturn(Optional.of(variante));
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // 2 × $30.000 = $60.000 de productos: el cupón se valida contra eso.
        when(cuponService.aplicar("verano10", 60000.0)).thenReturn(new ICuponService.CuponAplicado(cupon, 6000.0));

        pedidoService.crear(new PedidoRequest(null, null, null, List.of(new ItemPedidoRequest(2L, 7L, 2)), "verano10"),
                UsuarioDetails.fromCliente(clienteCrear));

        ArgumentCaptor<Pedido> guardado = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(guardado.capture());
        assertThat(guardado.getValue().getCupon()).isSameAs(cupon);
        assertThat(guardado.getValue().getDescuentoCupon()).isEqualTo(6000.0);
        assertThat(pedidoService.calcularTotal(guardado.getValue())).isEqualTo(54000.0);
    }

    @Test
    void crear_sinCupon_noLoBusca() {
        Producto producto = proteina();
        when(clienteRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(clienteCrear));
        when(productoRepository.findById(2L)).thenReturn(Optional.of(producto));
        when(varianteRepository.findById(7L)).thenReturn(Optional.of(Variante.builder().id(7L).producto(producto).stock(10).build()));
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        pedidoService.crear(new PedidoRequest(null, null, null, List.of(new ItemPedidoRequest(2L, 7L, 1)), "  "),
                UsuarioDetails.fromCliente(clienteCrear));

        verifyNoInteractions(cuponService);
    }

    @Test
    void cancelar_pedidoConCupon_devuelveElUso() {
        Cupon cupon = Cupon.builder().id(3L).codigo("VERANO10").usos(1).build();
        Pedido pedido = Pedido.builder().id(1L).cliente(clienteCrear).estadoPedido(EstadoPedido.PENDIENTE_PAGO).cupon(cupon).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                UsuarioDetails.fromCliente(clienteCrear), null, List.of()));
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        pedidoService.cancelar(1L);

        verify(cuponService).devolverUso(cupon);
        assertThat(pedido.getEstadoPedido()).isEqualTo(EstadoPedido.CANCELADO);
    }

    // ─── crear: costo de envío cotizado por el back ───────────────────────────

    private Direccion direccionDe(Cliente cliente) {
        return Direccion.builder().id(5L).cliente(cliente).build();
    }

    @Test
    void crear_conDireccion_guardaElCostoQueCotizaElBack() {
        Producto producto = proteina();
        Variante variante = Variante.builder().id(7L).producto(producto).stock(10).activo(true).build();
        OpcionEnvioRequest opcion = new OpcionEnvioRequest("D", "Clasico");
        when(clienteRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(clienteCrear));
        when(productoRepository.findById(2L)).thenReturn(Optional.of(producto));
        when(varianteRepository.findById(7L)).thenReturn(Optional.of(variante));
        when(direccionRepository.findById(5L)).thenReturn(Optional.of(direccionDe(clienteCrear)));
        when(cotizacionEnvioService.precioDeOpcion(5L, opcion)).thenReturn(4300.0);
        when(pedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        pedidoService.crear(new PedidoRequest(null, 5L, opcion, List.of(new ItemPedidoRequest(2L, 7L, 1)), null),
                UsuarioDetails.fromCliente(clienteCrear));

        ArgumentCaptor<Pedido> guardado = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(guardado.capture());
        assertThat(guardado.getValue().getCostoEnvio()).isEqualTo(4300.0);
    }

    @Test
    void crear_conDireccionSinOpcionDeEnvio_rechaza() {
        Producto producto = proteina();
        Variante variante = Variante.builder().id(7L).producto(producto).stock(10).activo(true).build();
        when(clienteRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(clienteCrear));
        when(productoRepository.findById(2L)).thenReturn(Optional.of(producto));
        when(varianteRepository.findById(7L)).thenReturn(Optional.of(variante));
        when(direccionRepository.findById(5L)).thenReturn(Optional.of(direccionDe(clienteCrear)));

        assertThatThrownBy(() -> pedidoService.crear(
                new PedidoRequest(null, 5L, null, List.of(new ItemPedidoRequest(2L, 7L, 1)), null),
                UsuarioDetails.fromCliente(clienteCrear)))
                .isInstanceOf(EcommerceException.class)
                .hasMessageContaining("opción de envío");
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void calcularTotal_sumaProductosMasEnvio() {
        Pedido pedido = pedidoConEstado(EstadoPedido.PENDIENTE_PAGO, itemDe(500.0, variante(10), 2));
        pedido.setCostoEnvio(1200.0);

        assertThat(pedidoService.calcularTotal(pedido)).isEqualTo(1700.0);
    }

    // ─── obtenerFacturaPdf() ─────────────────────────────────────────────────

    @Test
    void obtenerFacturaPdf_delDuenio_devuelveElPdf() {
        Pedido pedido = pedidoConEstado(EstadoPedido.PAGADO);
        Factura factura = Factura.builder().id(5L).pedido(pedido).tipoFactura(TipoFactura.B).precioTotal(100.0).build();
        pedido.setFactura(factura);
        byte[] contenido = "%PDF-prueba".getBytes();

        autenticarComoCliente(1L);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(facturaPdfGenerator.generar(factura)).thenReturn(contenido);
        when(facturaPdfGenerator.nombreArchivo(factura)).thenReturn("factura-B-0001-00000005.pdf");

        FacturaPdfResponse pdf = pedidoService.obtenerFacturaPdf(1L);

        assertThat(pdf.nombreArchivo()).isEqualTo("factura-B-0001-00000005.pdf");
        assertThat(pdf.contenido()).isEqualTo(contenido);
    }

    @Test
    void obtenerFacturaPdf_deOtroCliente_rechaza() {
        Pedido pedido = pedidoConEstado(EstadoPedido.PAGADO);
        pedido.setFactura(Factura.builder().id(5L).pedido(pedido).tipoFactura(TipoFactura.B).precioTotal(100.0).build());

        autenticarComoCliente(2L);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.obtenerFacturaPdf(1L)).isInstanceOf(AccessDeniedException.class);
        verify(facturaPdfGenerator, never()).generar(any());
    }

    @Test
    void obtenerFacturaPdf_sinFactura_avisaQueNoEstaPago() {
        autenticarComoCliente(1L);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoConEstado(EstadoPedido.PENDIENTE_PAGO)));

        assertThatThrownBy(() -> pedidoService.obtenerFacturaPdf(1L))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("aún no tiene factura");
    }

    // ─── confirmarRecepcion() (RF03) ─────────────────────────────────────────

    @Test
    void confirmarRecepcion_recibido_loGuardaConFecha() {
        autenticarComoCliente(1L);
        Pedido pedido = pedidoConEstado(EstadoPedido.ENTREGADO);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(pedido)).thenReturn(pedido);

        pedidoService.confirmarRecepcion(1L, new ConfirmarRecepcionRequest(true, "ignorado"));

        assertThat(pedido.getConfirmacionRecepcion()).isEqualTo(ConfirmacionRecepcion.RECIBIDO);
        assertThat(pedido.getFechaConfirmacionRecepcion()).isNotNull();
        assertThat(pedido.getComentarioRecepcion()).isNull();
    }

    @Test
    void confirmarRecepcion_noRecibido_guardaElComentario() {
        autenticarComoCliente(1L);
        Pedido pedido = pedidoConEstado(EstadoPedido.ENTREGADO);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(pedido)).thenReturn(pedido);

        pedidoService.confirmarRecepcion(1L, new ConfirmarRecepcionRequest(false, "  No vino nadie  "));

        assertThat(pedido.getConfirmacionRecepcion()).isEqualTo(ConfirmacionRecepcion.NO_RECIBIDO);
        assertThat(pedido.getComentarioRecepcion()).isEqualTo("No vino nadie");
    }

    @Test
    void confirmarRecepcion_noRecibidoYDespuesRecibido_seActualiza() {
        autenticarComoCliente(1L);
        Pedido pedido = pedidoConEstado(EstadoPedido.ENTREGADO);
        pedido.setConfirmacionRecepcion(ConfirmacionRecepcion.NO_RECIBIDO);
        pedido.setComentarioRecepcion("No llegó");
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(pedido)).thenReturn(pedido);

        pedidoService.confirmarRecepcion(1L, new ConfirmarRecepcionRequest(true, null));

        assertThat(pedido.getConfirmacionRecepcion()).isEqualTo(ConfirmacionRecepcion.RECIBIDO);
        assertThat(pedido.getComentarioRecepcion()).isNull();
    }

    @Test
    void confirmarRecepcion_yaRecibido_lanzaConflicto() {
        autenticarComoCliente(1L);
        Pedido pedido = pedidoConEstado(EstadoPedido.ENTREGADO);
        pedido.setConfirmacionRecepcion(ConfirmacionRecepcion.RECIBIDO);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.confirmarRecepcion(1L, new ConfirmarRecepcionRequest(false, null)))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("Ya confirmaste");
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void confirmarRecepcion_pedidoNoEntregado_lanzaNoModificable() {
        autenticarComoCliente(1L);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoConEstado(EstadoPedido.DESPACHADO)));

        assertThatThrownBy(() -> pedidoService.confirmarRecepcion(1L, new ConfirmarRecepcionRequest(true, null)))
                .isInstanceOf(PedidoNoModificableException.class)
                .hasMessageContaining("entregado");
    }

    @Test
    void confirmarRecepcion_deOtroCliente_lanzaAccessDenied() {
        autenticarComoCliente(2L);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoConEstado(EstadoPedido.ENTREGADO)));

        assertThatThrownBy(() -> pedidoService.confirmarRecepcion(1L, new ConfirmarRecepcionRequest(true, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void confirmarRecepcion_admin_lanzaAccessDenied() {
        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoConEstado(EstadoPedido.ENTREGADO)));

        assertThatThrownBy(() -> pedidoService.confirmarRecepcion(1L, new ConfirmarRecepcionRequest(true, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Pedido pedidoConEstado(EstadoPedido estado, ItemPedido... items) {
        Cliente cliente = Cliente.builder().id(1L).build();
        Pedido pedido = Pedido.builder()
                .id(1L)
                .cliente(cliente)
                .estadoPedido(estado)
                .build();
        for (ItemPedido item : items) {
            pedido.getItems().add(item);
        }
        return pedido;
    }

    private Variante variante(int stock) {
        return Variante.builder().id(1L).stock(stock).build();
    }

    private ItemPedido itemDe(double precioTotal, Variante variante, int cantidad) {
        return ItemPedido.builder().variante(variante).cantidad(cantidad).precioProducto(precioTotal).build();
    }

    private void autenticarComoCliente(Long id) {
        UsuarioDetails cliente = UsuarioDetails.fromCliente(
                Cliente.builder().id(id).email("c@test.com").contrasenia("x").build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(cliente, null, cliente.getAuthorities()));
    }

    private void autenticarComoAdmin() {
        UsuarioDetails admin = UsuarioDetails.fromAdmin(
                Administrador.builder().id(99L).username("admin").contrasenia("x").build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));
    }
}
