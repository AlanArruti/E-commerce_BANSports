package com.BlandiArruti.E_commerce.pago.pasarela.mercadopago;

import com.BlandiArruti.E_commerce.enums.EstadoPago;
import com.BlandiArruti.E_commerce.pago.pasarela.dto.CobroIniciado;
import com.BlandiArruti.E_commerce.pago.pasarela.dto.ResultadoCobro;
import com.BlandiArruti.E_commerce.pago.pasarela.dto.SolicitudCobro;
import com.BlandiArruti.E_commerce.pago.pasarela.mercadopago.dto.OrdenMercadoPago;
import com.BlandiArruti.E_commerce.pago.pasarela.mercadopago.dto.OrdenRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MercadoPagoPasarelaTest {

    @Mock MercadoPagoProperties properties;
    @Mock MercadoPagoClient client;

    @InjectMocks
    MercadoPagoPasarela pasarela;

    private OrdenMercadoPago orden(String status, String detalle, String pagado) {
        return new OrdenMercadoPago("ORD1", status, detalle, "15", "1500.50", pagado,
                "https://www.mercadopago.com.ar/checkout/v1/redirect?order_id=ORD1");
    }

    @Test
    void crearCobro_armaLaOrderConUnaLineaPorProductoYElTotalExacto() {
        when(properties.estaConfigurado()).thenReturn(true);
        when(client.crearOrden(any())).thenReturn(orden("created", "created", null));
        SolicitudCobro solicitud = new SolicitudCobro("15", "Pedido #3", List.of(
                new SolicitudCobro.LineaCobro("V7", "Whey Protein", 3, new BigDecimal("1000.333")),
                new SolicitudCobro.LineaCobro("P2", "Creatina", 1, new BigDecimal("500.17"))),
                "http://localhost:4200/pago/resultado?pago=15");

        CobroIniciado cobro = pasarela.iniciarCobro(solicitud);

        ArgumentCaptor<OrdenRequest> enviada = ArgumentCaptor.forClass(OrdenRequest.class);
        verify(client).crearOrden(enviada.capture());
        OrdenRequest orden = enviada.getValue();
        assertThat(orden.type()).isEqualTo("online");
        assertThat(orden.processingMode()).isEqualTo("manual");
        assertThat(orden.externalReference()).isEqualTo("15");
        assertThat(orden.items()).extracting(OrdenRequest.Item::title)
                .containsExactly("Whey Protein (x3)", "Creatina");
        assertThat(orden.items()).allMatch(i -> i.quantity() == 1);
        // La API exige que la suma de los ítems sea igual al total.
        BigDecimal suma = orden.items().stream().map(i -> new BigDecimal(i.unitPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(suma).isEqualByComparingTo(new BigDecimal(orden.totalAmount()));
        assertThat(orden.config().online().successUrl()).endsWith("/pago/resultado?pago=15");
        assertThat(orden.config().online().autoReturn()).as("http local: sin vuelta automática").isNull();
        assertThat(cobro.idExterno()).isEqualTo("ORD1");
        assertThat(cobro.urlPago()).contains("order_id=ORD1");
    }

    @ParameterizedTest
    @CsvSource({
            "processed, accredited, APROBADO",
            "created, created, CREADO",
            "action_required, waiting_payment, PENDIENTE",
            "processing, in_process, PENDIENTE",
            "failed, rejected, RECHAZADO",
            "expired, expired, RECHAZADO",
            "canceled, canceled, RECHAZADO",
            "refunded, refunded, RECHAZADO"
    })
    void buscarCobro_traduceLosEstadosDeMercadoPago(String status, String detalle, EstadoPago esperado) {
        when(client.obtenerOrden("ORD1")).thenReturn(orden(status, detalle, "1500.50"));

        ResultadoCobro resultado = pasarela.consultarCobro("ORD1");

        assertThat(resultado.estado()).isEqualTo(esperado);
        assertThat(resultado.referencia()).isEqualTo("15");
        assertThat(resultado.montoPagado()).isEqualByComparingTo("1500.50");
    }

    @Test
    void leerNotificacion_topicoOrder_devuelveElIdDelBody() {
        assertThat(pasarela.leerNotificacion(
                Map.of("type", "order", "data", Map.of("id", "ORD1")), Map.of()))
                .contains("ORD1");
    }

    @Test
    void leerNotificacion_topicoOrderSoloEnLaUrl_devuelveElId() {
        assertThat(pasarela.leerNotificacion(null, Map.of("type", "order", "data.id", "ORD2")))
                .contains("ORD2");
    }

    @Test
    void leerNotificacion_otroTopico_seIgnora() {
        assertThat(pasarela.leerNotificacion(
                Map.of("type", "payment", "data", Map.of("id", "123")), Map.of()))
                .isEmpty();
    }

    @Test
    void sinAccessToken_noEstaDisponible() {
        when(properties.estaConfigurado()).thenReturn(false);

        assertThat(pasarela.estaDisponible()).isFalse();
        assertThat(pasarela.codigo()).isEqualTo("mercadopago");
    }
}
