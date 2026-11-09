package com.BlandiArruti.E_commerce.admin.service;

import com.BlandiArruti.E_commerce.admin.dto.response.MetricasAdminResponse;
import com.BlandiArruti.E_commerce.admin.dto.response.ResumenAdminResponse;
import com.BlandiArruti.E_commerce.admin.dto.response.VarianteStockBajoResponse;
import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.factura.repository.FacturaRepository;
import com.BlandiArruti.E_commerce.pedido.repository.ItemPedidoRepository;
import com.BlandiArruti.E_commerce.pedido.repository.PedidoRepository;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import com.BlandiArruti.E_commerce.producto.entity.Variante;
import com.BlandiArruti.E_commerce.producto.repository.ProductoRepository;
import com.BlandiArruti.E_commerce.producto.repository.VarianteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminResumenServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private VarianteRepository varianteRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private FacturaRepository facturaRepository;

    @Mock
    private ItemPedidoRepository itemPedidoRepository;

    @InjectMocks
    private AdminResumenService service;

    @Test
    void metricas_agrupaVentasPorDiaYCompletaConCeros() {
        LocalDateTime hoy = LocalDate.now().atTime(10, 0);
        when(facturaRepository.ventasEntre(any(), any(), any())).thenReturn(List.of(
                new Object[]{hoy, 1000.0},
                new Object[]{hoy.plusHours(2), 500.0},
                new Object[]{hoy.minusDays(2), 300.0}));
        when(facturaRepository.sumarVentasEntre(any(), any(), any())).thenReturn(900.0);
        when(facturaRepository.contarVentasEntre(any(), any(), any())).thenReturn(2L);
        when(pedidoRepository.contarPorEstado()).thenReturn(List.<Object[]>of(
                new Object[]{EstadoPedido.PAGADO, 4L},
                new Object[]{EstadoPedido.CANCELADO, 1L}));
        when(itemPedidoRepository.masVendidosEntre(any(), any(), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{10L, "Whey", 3L, 4500.0}));

        MetricasAdminResponse metricas = service.metricas(7);

        assertThat(metricas.ventasPorDia()).hasSize(7);
        assertThat(metricas.ventasPorDia().get(6).fecha()).isEqualTo(LocalDate.now());
        assertThat(metricas.ventasPorDia().get(6).total()).isEqualTo(1500.0);
        assertThat(metricas.ventasPorDia().get(6).pedidos()).isEqualTo(2);
        assertThat(metricas.ventasPorDia().get(4).total()).isEqualTo(300.0);
        assertThat(metricas.ventasPorDia().get(0).total()).isZero();
        assertThat(metricas.ventasPeriodo()).isEqualTo(1800.0);
        assertThat(metricas.pedidosPeriodo()).isEqualTo(3);
        assertThat(metricas.ventasPeriodoAnterior()).isEqualTo(900.0);
        assertThat(metricas.pedidosPeriodoAnterior()).isEqualTo(2);

        assertThat(metricas.pedidosPorEstado()).hasSize(EstadoPedido.values().length);
        assertThat(metricas.pedidosPorEstado().get(0).estado()).isEqualTo("PENDIENTE_PAGO");
        assertThat(metricas.pedidosPorEstado().get(0).cantidad()).isZero();
        assertThat(metricas.pedidosPorEstado().get(1).cantidad()).isEqualTo(4);

        assertThat(metricas.masVendidos()).hasSize(1);
        assertThat(metricas.masVendidos().get(0).nombre()).isEqualTo("Whey");
        assertThat(metricas.masVendidos().get(0).unidades()).isEqualTo(3);
    }

    @Test
    void metricas_periodoNoPermitido_rechaza() {
        assertThatThrownBy(() -> service.metricas(15)).isInstanceOf(EcommerceException.class);
    }

    @Test
    void resumen_sinDatos_devuelveCeros() {
        when(facturaRepository.sumarVentasEntre(any(), any(), any())).thenReturn(0.0);
        when(pedidoRepository.countByEstadoPedido(any())).thenReturn(0L);
        when(varianteRepository.countByActivoTrueAndStock(0)).thenReturn(0L);
        when(varianteRepository.countByActivoTrueAndStockBetween(1, 5)).thenReturn(0L);
        when(productoRepository.countOfertasActivas()).thenReturn(0L);

        ResumenAdminResponse resumen = service.resumen();

        assertThat(resumen.ventasMes()).isEqualTo(0.0);
        assertThat(resumen.pendientesPago()).isZero();
        assertThat(resumen.enPreparacion()).isZero();
        assertThat(resumen.paraDespachar()).isZero();
        assertThat(resumen.sinStock()).isZero();
        assertThat(resumen.stockBajo()).isZero();
        assertThat(resumen.ofertasActivas()).isZero();
    }

    @Test
    void variantesStockBajo_mapeaNombreYProducto() {
        Variante variante = Variante.builder()
                .id(1L)
                .stock(3)
                .producto(Producto.builder().id(10L).nombre("Whey").build())
                .atributos(Map.of("sabor", "Chocolate"))
                .build();
        when(varianteRepository.findStockBajo(5)).thenReturn(List.of(variante));

        List<VarianteStockBajoResponse> resultado = service.variantesStockBajo();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).idVariante()).isEqualTo(1L);
        assertThat(resultado.get(0).idProducto()).isEqualTo(10L);
        assertThat(resultado.get(0).nombreProducto()).isEqualTo("Whey");
        assertThat(resultado.get(0).stock()).isEqualTo(3);
    }
}
