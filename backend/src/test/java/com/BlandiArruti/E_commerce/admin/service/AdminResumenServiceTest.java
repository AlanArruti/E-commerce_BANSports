package com.BlandiArruti.E_commerce.admin.service;

import com.BlandiArruti.E_commerce.admin.dto.response.ResumenAdminResponse;
import com.BlandiArruti.E_commerce.admin.dto.response.VarianteStockBajoResponse;
import com.BlandiArruti.E_commerce.factura.repository.FacturaRepository;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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

    @InjectMocks
    private AdminResumenService service;

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
