package com.BlandiArruti.E_commerce.pedido.service;

import com.BlandiArruti.E_commerce.administrador.entity.Administrador;
import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.entity.Direccion;
import com.BlandiArruti.E_commerce.cliente.repository.ClienteRepository;
import com.BlandiArruti.E_commerce.cliente.repository.DireccionRepository;
import com.BlandiArruti.E_commerce.envio.entity.Envio;
import com.BlandiArruti.E_commerce.envio.mapper.EnvioMapper;
import com.BlandiArruti.E_commerce.envio.repository.EnvioRepository;
import com.BlandiArruti.E_commerce.enums.EstadoEnvio;
import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.enums.TipoFactura;
import com.BlandiArruti.E_commerce.exception.ConflictoException;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.exception.PedidoNoModificableException;
import com.BlandiArruti.E_commerce.factura.mapper.FacturaMapper;
import com.BlandiArruti.E_commerce.factura.repository.FacturaRepository;
import com.BlandiArruti.E_commerce.notificacion.service.NotificacionService;
import com.BlandiArruti.E_commerce.pedido.dto.request.PagoRequest;
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

        pedidoService.cancelar(1L);

        assertThat(variante.getStock()).isEqualTo(7);
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
    void pagar_pedidoYaPagado_lanzaNoModificable() {
        Pedido pedido = pedidoConEstado(EstadoPedido.PAGADO);

        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.pagar(1L, new PagoRequest(TipoFactura.B)))
                .isInstanceOf(PedidoNoModificableException.class);
        verify(facturaRepository, never()).save(any());
    }

    // ─── crearEnvio() ────────────────────────────────────────────────────────

    @Test
    void crearEnvio_cuandoYaExisteEnvio_lanzaConflicto() {
        Pedido pedido = pedidoConEstado(EstadoPedido.PAGADO);

        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(envioRepository.findByPedidoId(1L)).thenReturn(Optional.of(Envio.builder().build()));

        var request = new EnvioRequest(LocalDate.now(), LocalDate.now().plusDays(3));

        assertThatThrownBy(() -> pedidoService.crearEnvio(1L, request))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void crearEnvio_pedidoNoPagado_lanzaExcepcion() {
        Pedido pedido = pedidoConEstado(EstadoPedido.PENDIENTE_PAGO);

        autenticarComoAdmin();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        var request = new EnvioRequest(LocalDate.now(), LocalDate.now().plusDays(3));

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

        pedidoService.crearEnvio(1L, new EnvioRequest(LocalDate.now(), LocalDate.now().plusDays(3)));

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

    private final Cliente clienteCrear = Cliente.builder().id(1L).email("c@test.com").contrasenia("x").activo(true).build();

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

        pedidoService.crear(new PedidoRequest(null, null, List.of(new ItemPedidoRequest(2L, 7L, 2))),
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
                new PedidoRequest(null, null, List.of(new ItemPedidoRequest(2L, 7L, 1))),
                UsuarioDetails.fromCliente(clienteCrear)))
                .isInstanceOf(EcommerceException.class)
                .hasMessageContaining("no pertenece al producto");
        verify(pedidoRepository, never()).save(any());
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
