package com.BlandiArruti.E_commerce.dolar.service;

import com.BlandiArruti.E_commerce.dolar.client.DolarApiClient;
import com.BlandiArruti.E_commerce.dolar.client.dto.DolarApiResponse;
import com.BlandiArruti.E_commerce.dolar.config.DolarProperties;
import com.BlandiArruti.E_commerce.dolar.dto.response.ActualizacionDolarResponse;
import com.BlandiArruti.E_commerce.dolar.dto.response.ActualizacionDolarResponse.ResultadoAjuste;
import com.BlandiArruti.E_commerce.dolar.dto.response.CotizacionDolarResponse;
import com.BlandiArruti.E_commerce.dolar.entity.CotizacionDolar;
import com.BlandiArruti.E_commerce.dolar.repository.CotizacionDolarRepository;
import com.BlandiArruti.E_commerce.exception.ServicioExternoException;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import com.BlandiArruti.E_commerce.producto.entity.Variante;
import com.BlandiArruti.E_commerce.producto.repository.ProductoRepository;
import com.BlandiArruti.E_commerce.producto.repository.VarianteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DolarServiceTest {

    @Mock DolarApiClient dolarApiClient;
    @Mock CotizacionDolarRepository cotizacionDolarRepository;
    @Mock ProductoRepository productoRepository;
    @Mock VarianteRepository varianteRepository;
    // Sin comportamiento: TransactionTemplate le pide la transacción, ejecuta el callback y hace commit.
    @Mock PlatformTransactionManager transactionManager;

    DolarProperties properties;
    DolarService service;

    /** Hace de tabla cotizaciones_dolar: lo que se guarda se puede volver a leer. */
    final List<CotizacionDolar> tabla = new ArrayList<>();

    Producto producto;
    Variante varianteConPrecio;
    Variante varianteSinPrecio;

    @BeforeEach
    void setUp() {
        properties = new DolarProperties();
        properties.setCasa("oficial");
        properties.setAjusteMinimoPorcentaje(1.0);
        properties.setVariacionMaximaPorcentaje(20.0);

        service = new DolarService(dolarApiClient, properties, cotizacionDolarRepository,
                productoRepository, varianteRepository, transactionManager);

        when(cotizacionDolarRepository.save(any(CotizacionDolar.class))).thenAnswer(inv -> {
            CotizacionDolar c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId((long) tabla.size() + 1);
                tabla.add(c);
            }
            return c;
        });
        when(cotizacionDolarRepository.findFirstByOrderByIdDesc()).thenAnswer(inv ->
                tabla.stream().max(Comparator.comparing(CotizacionDolar::getId)));
        when(cotizacionDolarRepository.findFirstByAplicadaTrueOrderByIdDesc()).thenAnswer(inv ->
                tabla.stream().filter(CotizacionDolar::isAplicada).max(Comparator.comparing(CotizacionDolar::getId)));

        producto = Producto.builder().id(1L).precio(10000.0).descuento(20).build();
        producto.recalcularPrecioFinal();
        varianteConPrecio = Variante.builder().id(10L).producto(producto).precio(15000.0).stock(5).build();
        varianteSinPrecio = Variante.builder().id(11L).producto(producto).precio(null).stock(5).build();
        when(productoRepository.findAll()).thenReturn(List.of(producto));
        when(varianteRepository.findAll()).thenReturn(List.of(varianteConPrecio, varianteSinPrecio));
    }

    private void dolarEn(double venta) {
        when(dolarApiClient.obtenerCotizacion()).thenReturn(
                new DolarApiResponse("oficial", "Oficial", venta - 50, venta, OffsetDateTime.now()));
    }

    @Test
    void primeraCotizacion_quedaComoBase_yNoTocaPrecios() {
        dolarEn(1000);

        ActualizacionDolarResponse r = service.actualizar(false);

        assertThat(r.resultado()).isEqualTo(ResultadoAjuste.BASE);
        assertThat(tabla).hasSize(1);
        assertThat(tabla.getFirst().isAplicada()).isTrue();
        assertThat(producto.getPrecio()).isEqualTo(10000.0);
        verify(productoRepository, never()).saveAll(any());
    }

    @Test
    void dolarSube10PorCiento_ajustaProductosYVariantesConPrecioPropio() {
        dolarEn(1000);
        service.actualizar(false);

        dolarEn(1100);
        ActualizacionDolarResponse r = service.actualizar(false);

        assertThat(r.resultado()).isEqualTo(ResultadoAjuste.AJUSTADO);
        assertThat(r.porcentajeAjuste()).isEqualTo(10.0);
        assertThat(r.productosAjustados()).isEqualTo(1);
        assertThat(producto.getPrecio()).isEqualTo(11000.0);
        // El descuento del 20% se sigue aplicando sobre el precio nuevo.
        assertThat(producto.getPrecioFinal()).isEqualTo(8800.0);
        assertThat(varianteConPrecio.getPrecio()).isEqualTo(16500.0);
        assertThat(varianteSinPrecio.getPrecio()).isNull();
        // La nueva cotización pasa a ser la referencia.
        assertThat(r.cotizacion().cotizacionReferencia()).isEqualTo(1100.0);
        assertThat(r.cotizacion().variacionPorcentaje()).isEqualTo(0.0);
    }

    @Test
    void dolarBaja_tambienAjustaHaciaAbajo() {
        dolarEn(1000);
        service.actualizar(false);

        dolarEn(950);
        ActualizacionDolarResponse r = service.actualizar(false);

        assertThat(r.resultado()).isEqualTo(ResultadoAjuste.AJUSTADO);
        assertThat(r.porcentajeAjuste()).isEqualTo(-5.0);
        assertThat(producto.getPrecio()).isEqualTo(9500.0);
    }

    @Test
    void variacionMenorAlMinimo_noTocaPrecios_peroSeAcumula() {
        dolarEn(1000);
        service.actualizar(false);

        dolarEn(1005); // +0,5%
        ActualizacionDolarResponse r = service.actualizar(false);
        assertThat(r.resultado()).isEqualTo(ResultadoAjuste.SIN_CAMBIOS);
        assertThat(producto.getPrecio()).isEqualTo(10000.0);
        assertThat(r.cotizacion().variacionPorcentaje()).isEqualTo(0.5);

        // Se compara siempre contra la referencia (1000), no contra la última leída (1005).
        dolarEn(1012); // +1,2% desde la referencia
        ActualizacionDolarResponse r2 = service.actualizar(false);
        assertThat(r2.resultado()).isEqualTo(ResultadoAjuste.AJUSTADO);
        assertThat(producto.getPrecio()).isEqualTo(10120.0);
    }

    @Test
    void saltoMayorAlMaximo_noSeAplicaSolo_peroElAdminPuedeForzarlo() {
        dolarEn(1000);
        service.actualizar(false);

        dolarEn(1500); // +50%
        ActualizacionDolarResponse r = service.actualizar(false);
        assertThat(r.resultado()).isEqualTo(ResultadoAjuste.REQUIERE_CONFIRMACION);
        assertThat(producto.getPrecio()).isEqualTo(10000.0);

        ActualizacionDolarResponse forzado = service.actualizar(true);
        assertThat(forzado.resultado()).isEqualTo(ResultadoAjuste.AJUSTADO);
        assertThat(producto.getPrecio()).isEqualTo(15000.0);
    }

    @Test
    void mismoValor_noCreaOtraFila_niAjusta() {
        dolarEn(1000);
        service.actualizar(false);

        dolarEn(1000);
        ActualizacionDolarResponse r = service.actualizar(false);

        assertThat(r.resultado()).isEqualTo(ResultadoAjuste.SIN_CAMBIOS);
        assertThat(tabla).hasSize(1);
        verify(productoRepository, never()).saveAll(any());
    }

    @Test
    void cambioDeTipoDeDolar_quedaComoNuevaBase_sinAjustar() {
        dolarEn(1000);
        service.actualizar(false);

        when(dolarApiClient.obtenerCotizacion()).thenReturn(
                new DolarApiResponse("blue", "Blue", 1400.0, 1450.0, OffsetDateTime.now()));
        ActualizacionDolarResponse r = service.actualizar(false);

        assertThat(r.resultado()).isEqualTo(ResultadoAjuste.BASE);
        assertThat(producto.getPrecio()).isEqualTo(10000.0);
    }

    @Test
    void apiCaida_noGuardaNada() {
        when(dolarApiClient.obtenerCotizacion()).thenThrow(new ServicioExternoException("caída"));

        assertThatThrownBy(() -> service.actualizar(false)).isInstanceOf(ServicioExternoException.class);
        assertThat(tabla).isEmpty();
    }

    @Test
    void obtenerActual_sinCotizacionTodavia_esServicioExterno() {
        assertThatThrownBy(() -> service.obtenerActual()).isInstanceOf(ServicioExternoException.class);
    }

    @Test
    void obtenerActual_muestraLaVariacionDesdeElUltimoAjuste() {
        dolarEn(1000);
        service.actualizar(false);
        dolarEn(1005);
        service.actualizar(false);

        CotizacionDolarResponse actual = service.obtenerActual();

        assertThat(actual.venta()).isEqualTo(1005.0);
        assertThat(actual.cotizacionReferencia()).isEqualTo(1000.0);
        assertThat(actual.variacionPorcentaje()).isEqualTo(0.5);
        assertThat(actual.fechaUltimoAjuste()).isNotNull();
        // El front los usa para la barra de progreso y el aviso de salto grande.
        assertThat(actual.ajusteMinimoPorcentaje()).isEqualTo(1.0);
        assertThat(actual.variacionMaximaPorcentaje()).isEqualTo(20.0);
    }

    @Test
    void variacionPorcentaje_calculaSubidasYBajadas() {
        assertThat(DolarService.variacionPorcentaje(1000, 1100)).isCloseTo(10.0, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(DolarService.variacionPorcentaje(1000, 900)).isCloseTo(-10.0, org.assertj.core.data.Offset.offset(1e-9));
    }
}
