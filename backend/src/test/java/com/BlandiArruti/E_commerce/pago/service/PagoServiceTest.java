package com.BlandiArruti.E_commerce.pago.service;

import com.BlandiArruti.E_commerce.enums.EstadoPago;
import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.enums.TipoFactura;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.pago.dto.request.IniciarPagoRequest;
import com.BlandiArruti.E_commerce.pago.dto.response.InicioPagoResponse;
import com.BlandiArruti.E_commerce.pago.dto.response.PagoResponse;
import com.BlandiArruti.E_commerce.pago.entity.Pago;
import com.BlandiArruti.E_commerce.pago.pasarela.PasarelaPago;
import com.BlandiArruti.E_commerce.pago.pasarela.RegistroPasarelas;
import com.BlandiArruti.E_commerce.pago.pasarela.dto.CobroIniciado;
import com.BlandiArruti.E_commerce.pago.pasarela.dto.ResultadoCobro;
import com.BlandiArruti.E_commerce.pago.pasarela.dto.SolicitudCobro;
import com.BlandiArruti.E_commerce.pago.repository.PagoRepository;
import com.BlandiArruti.E_commerce.pedido.entity.ItemPedido;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import com.BlandiArruti.E_commerce.pedido.service.IPedidoService;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import com.BlandiArruti.E_commerce.producto.entity.Variante;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

/** PagoService trabaja contra la interfaz PasarelaPago: acá se la reemplaza por un mock. */
@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock RegistroPasarelas registroPasarelas;
    @Mock PagoRepository pagoRepository;
    @Mock IPedidoService pedidoService;
    @Mock PasarelaPago pasarela;

    @InjectMocks
    PagoService pagoService;

    private Pedido pedido;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pagoService, "frontUrl", "http://localhost:4200");
        Producto producto = Producto.builder().id(2L).nombre("Whey Protein").build();
        Variante variante = Variante.builder().id(7L).build();
        pedido = Pedido.builder().id(3L).estadoPedido(EstadoPedido.PENDIENTE_PAGO).build();
        pedido.getItems().add(ItemPedido.builder()
                .producto(producto).variante(variante).cantidad(2).precioProducto(1000.0).build());
        lenient().when(pasarela.codigo()).thenReturn("falsa");
        lenient().when(registroPasarelas.obtener("falsa")).thenReturn(pasarela);
    }

    private Pago pagoCreado() {
        return Pago.builder().id(15L).pedido(pedido).pasarela("falsa").idExterno("EXT-1")
                .estado(EstadoPago.CREADO).tipoFactura(TipoFactura.B).monto(1000.0).build();
    }

    // ─── iniciar ─────────────────────────────────────────────────────────────

    @Test
    void iniciar_guardaElIntentoYPideElCobroConVueltaANuestroIdDePago() {
        when(pedidoService.obtenerParaPagar(3L)).thenReturn(pedido);
        when(pedidoService.calcularTotal(pedido)).thenReturn(1000.0);
        when(pagoRepository.save(any())).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            p.setId(15L);
            return p;
        });
        when(pasarela.iniciarCobro(any())).thenReturn(new CobroIniciado("EXT-1", "https://pasarela/pagar"));

        InicioPagoResponse respuesta = pagoService.iniciar(3L, new IniciarPagoRequest("falsa", TipoFactura.A));

        ArgumentCaptor<SolicitudCobro> solicitud = ArgumentCaptor.forClass(SolicitudCobro.class);
        verify(pasarela).iniciarCobro(solicitud.capture());
        assertThat(solicitud.getValue().referencia()).isEqualTo("15");
        assertThat(solicitud.getValue().urlVuelta()).isEqualTo("http://localhost:4200/pago/resultado?pago=15");
        assertThat(solicitud.getValue().lineas()).singleElement()
                .satisfies(l -> {
                    assertThat(l.codigo()).isEqualTo("V7");
                    assertThat(l.cantidad()).isEqualTo(2);
                    assertThat(l.monto()).isEqualByComparingTo("1000");
                });
        assertThat(respuesta.pagoId()).isEqualTo(15L);
        assertThat(respuesta.urlPago()).isEqualTo("https://pasarela/pagar");

        ArgumentCaptor<Pago> guardado = ArgumentCaptor.forClass(Pago.class);
        verify(pagoRepository).save(guardado.capture());
        assertThat(guardado.getValue().getEstado()).isEqualTo(EstadoPago.CREADO);
        assertThat(guardado.getValue().getTipoFactura()).isEqualTo(TipoFactura.A);
        assertThat(guardado.getValue().getIdExterno()).isEqualTo("EXT-1");
    }

    @Test
    void iniciar_conCostoDeEnvio_loCobraComoUnaLineaMas() {
        pedido.setCostoEnvio(4500.0);
        when(pedidoService.obtenerParaPagar(3L)).thenReturn(pedido);
        when(pedidoService.calcularTotal(pedido)).thenReturn(5500.0);
        when(pagoRepository.save(any())).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            p.setId(16L);
            return p;
        });
        when(pasarela.iniciarCobro(any())).thenReturn(new CobroIniciado("EXT-1", "https://pasarela/pagar"));

        pagoService.iniciar(3L, new IniciarPagoRequest("falsa", TipoFactura.B));

        ArgumentCaptor<SolicitudCobro> solicitud = ArgumentCaptor.forClass(SolicitudCobro.class);
        verify(pasarela).iniciarCobro(solicitud.capture());
        assertThat(solicitud.getValue().lineas()).hasSize(2);
        assertThat(solicitud.getValue().lineas().get(1).codigo()).isEqualTo("ENVIO");
        assertThat(solicitud.getValue().total()).isEqualByComparingTo("5500");
    }

    // ─── confirmar (pantalla de vuelta) ──────────────────────────────────────

    @Test
    void confirmar_cobroAprobado_pagaElPedidoYMarcaElPago() {
        Pago pago = pagoCreado();
        when(pagoRepository.findById(15L)).thenReturn(Optional.of(pago));
        when(pasarela.consultarCobro("EXT-1"))
                .thenReturn(new ResultadoCobro("EXT-1", EstadoPago.APROBADO, new BigDecimal("1000.00"), "15"));
        when(pedidoService.registrarPagoAprobado(3L, TipoFactura.B, new BigDecimal("1000.00"))).thenReturn(true);

        PagoResponse respuesta = pagoService.confirmar(15L);

        verify(pedidoService).verificarAcceso(pedido);
        assertThat(respuesta.estado()).isEqualTo(EstadoPago.APROBADO);
        assertThat(pago.getMontoPagado()).isEqualTo(1000.0);
    }

    @Test
    void confirmar_pagoYaAprobado_noVuelveAConsultarNiCobra() {
        Pago pago = pagoCreado();
        pago.setEstado(EstadoPago.APROBADO);
        when(pagoRepository.findById(15L)).thenReturn(Optional.of(pago));

        pagoService.confirmar(15L);

        verify(pasarela, never()).consultarCobro(any());
        verify(pedidoService, never()).registrarPagoAprobado(any(), any(), any());
    }

    @Test
    void confirmar_cobroRechazado_guardaElEstadoSinTocarElPedido() {
        Pago pago = pagoCreado();
        when(pagoRepository.findById(15L)).thenReturn(Optional.of(pago));
        when(pasarela.consultarCobro("EXT-1"))
                .thenReturn(new ResultadoCobro("EXT-1", EstadoPago.RECHAZADO, null, "15"));

        PagoResponse respuesta = pagoService.confirmar(15L);

        assertThat(respuesta.estado()).isEqualTo(EstadoPago.RECHAZADO);
        verify(pedidoService, never()).registrarPagoAprobado(any(), any(), any());
    }

    @Test
    void confirmar_cobroDeOtroPago_rechazaSinCobrar() {
        when(pagoRepository.findById(15L)).thenReturn(Optional.of(pagoCreado()));
        // Un cobro real y aprobado, pero creado para otro pago: no puede pagar este pedido.
        when(pasarela.consultarCobro("EXT-1"))
                .thenReturn(new ResultadoCobro("EXT-1", EstadoPago.APROBADO, new BigDecimal("1000"), "99"));

        assertThatThrownBy(() -> pagoService.confirmar(15L))
                .isInstanceOf(EcommerceException.class)
                .hasMessageContaining("no corresponde");
        verify(pedidoService, never()).registrarPagoAprobado(any(), any(), any());
    }

    @Test
    void confirmar_aprobadoPeroElPedidoNoLoAcepta_quedaRechazadoParaRevisar() {
        Pago pago = pagoCreado();
        when(pagoRepository.findById(15L)).thenReturn(Optional.of(pago));
        when(pasarela.consultarCobro("EXT-1"))
                .thenReturn(new ResultadoCobro("EXT-1", EstadoPago.APROBADO, new BigDecimal("10"), "15"));
        when(pedidoService.registrarPagoAprobado(any(), any(), any())).thenReturn(false);

        assertThat(pagoService.confirmar(15L).estado()).isEqualTo(EstadoPago.RECHAZADO);
    }

    // ─── procesarNotificacion (webhook) ──────────────────────────────────────

    @Test
    void notificacion_deUnCobroNuestro_loActualiza() {
        Pago pago = pagoCreado();
        when(pasarela.leerNotificacion(anyMap(), anyMap())).thenReturn(Optional.of("EXT-1"));
        when(pagoRepository.findByPasarelaAndIdExterno("falsa", "EXT-1")).thenReturn(Optional.of(pago));
        when(pasarela.consultarCobro("EXT-1"))
                .thenReturn(new ResultadoCobro("EXT-1", EstadoPago.APROBADO, new BigDecimal("1000"), "15"));
        when(pedidoService.registrarPagoAprobado(any(), any(), any())).thenReturn(true);

        pagoService.procesarNotificacion("falsa", Map.of(), Map.of());

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);
        // El webhook no verifica dueño: no hay cliente logueado.
        verify(pedidoService, never()).verificarAcceso(any());
    }

    @Test
    void notificacion_deUnCobroAjeno_seIgnora() {
        when(pasarela.leerNotificacion(anyMap(), anyMap())).thenReturn(Optional.of("EXT-X"));
        when(pagoRepository.findByPasarelaAndIdExterno("falsa", "EXT-X")).thenReturn(Optional.empty());

        pagoService.procesarNotificacion("falsa", Map.of(), Map.of());

        verify(pasarela, never()).consultarCobro(any());
    }

    @Test
    void notificacion_queNoEsDeUnCobro_noHaceNada() {
        when(pasarela.leerNotificacion(anyMap(), anyMap())).thenReturn(Optional.empty());

        pagoService.procesarNotificacion("falsa", Map.of(), Map.of());

        verifyNoInteractions(pagoRepository);
    }
}
