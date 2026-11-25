package com.BlandiArruti.E_commerce.pago.service;

import com.BlandiArruti.E_commerce.enums.EstadoPago;
import com.BlandiArruti.E_commerce.exception.ServicioExternoException;
import com.BlandiArruti.E_commerce.pago.entity.Pago;
import com.BlandiArruti.E_commerce.pago.pasarela.PasarelaPago;
import com.BlandiArruti.E_commerce.pago.pasarela.RegistroPasarelas;
import com.BlandiArruti.E_commerce.pago.repository.PagoRepository;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReembolsoPagosServiceTest {

    @Mock PagoRepository pagoRepository;
    @Mock RegistroPasarelas registroPasarelas;
    @Mock PasarelaPago mercadoPago;

    @InjectMocks
    ReembolsoPagosService service;

    private final Pedido pedido = Pedido.builder().id(3L).build();

    private Pago aprobado(String idExterno, Double monto, Double pagado) {
        return Pago.builder().id(15L).pedido(pedido).pasarela("mercadopago").idExterno(idExterno)
                .estado(EstadoPago.APROBADO).monto(monto).montoPagado(pagado).build();
    }

    @Test
    void reembolsaCadaCobroAprobado_yLoMarcaComoReembolsado() {
        Pago pago = aprobado("ORD1", 1500.0, 1500.0);
        when(pagoRepository.findByPedidoIdAndEstado(3L, EstadoPago.APROBADO)).thenReturn(List.of(pago));
        when(registroPasarelas.obtener("mercadopago")).thenReturn(mercadoPago);

        double total = service.reembolsarCobros(pedido);

        verify(mercadoPago).reembolsar("ORD1");
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.REEMBOLSADO);
        assertThat(total).isEqualTo(1500.0);
        verify(pagoRepository).save(pago);
    }

    @Test
    void sinCobrosEnPasarelas_devuelveCero() {
        when(pagoRepository.findByPedidoIdAndEstado(3L, EstadoPago.APROBADO)).thenReturn(List.of());

        assertThat(service.reembolsarCobros(pedido)).isZero();
        verifyNoInteractions(registroPasarelas);
    }

    @Test
    void siLaPasarelaRechaza_noMarcaElPagoYPropagaElError() {
        Pago pago = aprobado("ORD1", 1500.0, null);
        when(pagoRepository.findByPedidoIdAndEstado(3L, EstadoPago.APROBADO)).thenReturn(List.of(pago));
        when(registroPasarelas.obtener("mercadopago")).thenReturn(mercadoPago);
        doThrow(new ServicioExternoException("Mercado Pago rechazó la devolución")).when(mercadoPago).reembolsar("ORD1");

        assertThatThrownBy(() -> service.reembolsarCobros(pedido)).isInstanceOf(ServicioExternoException.class);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);
        verify(pagoRepository, never()).save(any());
    }
}
