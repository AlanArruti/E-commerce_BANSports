package com.BlandiArruti.E_commerce.dolar.service;

import com.BlandiArruti.E_commerce.dolar.client.DolarApiClient;
import com.BlandiArruti.E_commerce.dolar.client.dto.DolarApiResponse;
import com.BlandiArruti.E_commerce.dolar.config.DolarProperties;
import com.BlandiArruti.E_commerce.dolar.dto.response.ActualizacionDolarResponse;
import com.BlandiArruti.E_commerce.dolar.dto.response.ActualizacionDolarResponse.ResultadoAjuste;
import com.BlandiArruti.E_commerce.dolar.dto.response.AjustePreciosResponse;
import com.BlandiArruti.E_commerce.dolar.dto.response.CotizacionDolarResponse;
import com.BlandiArruti.E_commerce.dolar.entity.CotizacionDolar;
import com.BlandiArruti.E_commerce.dolar.repository.CotizacionDolarRepository;
import com.BlandiArruti.E_commerce.exception.ServicioExternoException;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import com.BlandiArruti.E_commerce.producto.entity.Variante;
import com.BlandiArruti.E_commerce.producto.repository.ProductoRepository;
import com.BlandiArruti.E_commerce.producto.repository.VarianteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * RF01: cotización del dólar en tiempo real y ajuste automático de precios.
 *
 * Los precios se cargan y se guardan en pesos. Se toma como referencia la cotización con la
 * que se ajustaron por última vez (la primera que se lee es la base). Cuando el dólar se aleja
 * de esa referencia al menos dolar.ajuste-minimo-porcentaje, todos los precios (de productos y
 * de variantes) se multiplican por nueva / referencia y esa cotización pasa a ser la referencia.
 * Si el salto supera dolar.variacion-maxima-porcentaje no se aplica solo: lo confirma el admin.
 */
@Slf4j
@Service
public class DolarService {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    private final DolarApiClient dolarApiClient;
    private final DolarProperties properties;
    private final CotizacionDolarRepository cotizacionDolarRepository;
    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final TransactionTemplate transactionTemplate;

    public DolarService(DolarApiClient dolarApiClient,
                        DolarProperties properties,
                        CotizacionDolarRepository cotizacionDolarRepository,
                        ProductoRepository productoRepository,
                        VarianteRepository varianteRepository,
                        PlatformTransactionManager transactionManager) {
        this.dolarApiClient = dolarApiClient;
        this.properties = properties;
        this.cotizacionDolarRepository = cotizacionDolarRepository;
        this.productoRepository = productoRepository;
        this.varianteRepository = varianteRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Consulta DolarApi, guarda la cotización y ajusta los precios si corresponde.
     *
     * @param forzar true = ajustar aunque la variación esté fuera de los márgenes configurados
     *               (lo usa el admin para confirmar un salto grande)
     */
    public ActualizacionDolarResponse actualizar(boolean forzar) {
        // La llamada HTTP va afuera de la transacción: no tiene sentido tener una conexión a la
        // base abierta mientras esperamos a la API.
        DolarApiResponse cotizacion = dolarApiClient.obtenerCotizacion();
        // synchronized + la transacción adentro: si el scheduler y el admin actualizan a la vez,
        // el segundo ve el ajuste del primero ya confirmado y no ajusta los precios dos veces.
        synchronized (this) {
            return transactionTemplate.execute(status -> registrar(cotizacion, forzar));
        }
    }

    @Transactional(readOnly = true)
    public CotizacionDolarResponse obtenerActual() {
        CotizacionDolar actual = cotizacionDolarRepository.findFirstByOrderByIdDesc()
                .orElseThrow(() -> new ServicioExternoException(
                        "Todavía no tenemos la cotización del dólar. Probá de nuevo en unos minutos."));
        return toResponse(actual, cotizacionDolarRepository.findFirstByAplicadaTrueOrderByIdDesc());
    }

    @Transactional(readOnly = true)
    public List<AjustePreciosResponse> historialAjustes() {
        return cotizacionDolarRepository.findTop50ByAplicadaTrueOrderByIdDesc().stream()
                .map(c -> new AjustePreciosResponse(c.getCasa(), c.getVenta(), c.getPorcentajeAjuste(),
                        c.getProductosAjustados(), c.getFechaAjuste()))
                .toList();
    }

    private ActualizacionDolarResponse registrar(DolarApiResponse api, boolean forzar) {
        OffsetDateTime ahora = OffsetDateTime.now(ZONA);
        CotizacionDolar actual = guardarCotizacion(api, ahora);
        Optional<CotizacionDolar> referencia = cotizacionDolarRepository.findFirstByAplicadaTrueOrderByIdDesc();

        // Primera cotización, o se cambió el tipo de dólar en la configuración: no hay contra
        // qué comparar, así que pasa a ser la referencia sin tocar los precios.
        if (referencia.isEmpty() || !referencia.get().getCasa().equals(actual.getCasa())) {
            marcarAplicada(actual, 0.0, 0, ahora);
            log.info("Dólar {}: ${} queda como cotización de referencia de los precios.",
                    actual.getCasa(), actual.getVenta());
            return resultado(actual, Optional.of(actual), ResultadoAjuste.BASE, 0.0, 0,
                    "Primera cotización: queda como referencia y los precios no cambian.");
        }

        CotizacionDolar ref = referencia.get();
        double variacion = variacionPorcentaje(ref.getVenta(), actual.getVenta());

        if (ref.getId().equals(actual.getId()) || variacion == 0.0) {
            return resultado(actual, referencia, ResultadoAjuste.SIN_CAMBIOS, 0.0, 0,
                    "El dólar no cambió desde el último ajuste de precios.");
        }
        if (!forzar && Math.abs(variacion) < properties.getAjusteMinimoPorcentaje()) {
            return resultado(actual, referencia, ResultadoAjuste.SIN_CAMBIOS, 0.0, 0,
                    "El dólar se movió " + formatear(variacion) + "% desde el último ajuste, menos que el mínimo de "
                            + formatear(properties.getAjusteMinimoPorcentaje()) + "%: los precios no cambian.");
        }
        if (!forzar && Math.abs(variacion) > properties.getVariacionMaximaPorcentaje()) {
            log.warn("Dólar {}: pasó de ${} a ${} ({}%). Supera el máximo de {}%: los precios no se ajustan solos.",
                    actual.getCasa(), ref.getVenta(), actual.getVenta(), formatear(variacion),
                    formatear(properties.getVariacionMaximaPorcentaje()));
            return resultado(actual, referencia, ResultadoAjuste.REQUIERE_CONFIRMACION, 0.0, 0,
                    "El dólar se movió " + formatear(variacion) + "%, más que el máximo de "
                            + formatear(properties.getVariacionMaximaPorcentaje())
                            + "%. Si es correcto, confirmá el ajuste con forzar=true.");
        }

        BigDecimal factor = BigDecimal.valueOf(actual.getVenta())
                .divide(BigDecimal.valueOf(ref.getVenta()), 10, RoundingMode.HALF_UP);
        int productosAjustados = ajustarPrecios(factor);
        marcarAplicada(actual, variacion, productosAjustados, ahora);
        log.info("Dólar {}: ${} -> ${} ({}%). Se ajustaron los precios de {} productos.",
                actual.getCasa(), ref.getVenta(), actual.getVenta(), formatear(variacion), productosAjustados);
        return resultado(actual, Optional.of(actual), ResultadoAjuste.AJUSTADO, variacion, productosAjustados,
                "Se ajustaron los precios un " + formatear(variacion) + "%.");
    }

    /** Guarda una fila nueva si el valor cambió; si no, solo actualiza la fecha de consulta de la última. */
    private CotizacionDolar guardarCotizacion(DolarApiResponse api, OffsetDateTime ahora) {
        String casa = api.casa() != null ? api.casa() : properties.getCasa();
        Optional<CotizacionDolar> ultima = cotizacionDolarRepository.findFirstByOrderByIdDesc();
        if (ultima.isPresent()
                && ultima.get().getCasa().equals(casa)
                && ultima.get().getVenta().equals(api.venta())
                && Objects.equals(ultima.get().getCompra(), api.compra())) {
            CotizacionDolar misma = ultima.get();
            misma.setFechaConsulta(ahora);
            misma.setFechaActualizacion(api.fechaActualizacion());
            return cotizacionDolarRepository.save(misma);
        }
        return cotizacionDolarRepository.save(CotizacionDolar.builder()
                .casa(casa)
                .compra(api.compra())
                .venta(api.venta())
                .fechaActualizacion(api.fechaActualizacion())
                .fechaConsulta(ahora)
                .build());
    }

    /** Multiplica el precio de todos los productos y variantes (activos o no) por el factor. */
    private int ajustarPrecios(BigDecimal factor) {
        List<Producto> productos = productoRepository.findAll();
        for (Producto producto : productos) {
            if (producto.getPrecio() != null) {
                producto.setPrecio(aplicarFactor(producto.getPrecio(), factor));
            }
            producto.recalcularPrecioFinal();
        }
        productoRepository.saveAll(productos);

        // Variantes con precio propio (ej. la de 2 kg). Las que no tienen usan el del producto.
        List<Variante> variantes = varianteRepository.findAll().stream()
                .filter(v -> v.getPrecio() != null)
                .toList();
        for (Variante variante : variantes) {
            variante.setPrecio(aplicarFactor(variante.getPrecio(), factor));
        }
        varianteRepository.saveAll(variantes);

        return productos.size();
    }

    private void marcarAplicada(CotizacionDolar cotizacion, double porcentaje, int productos, OffsetDateTime ahora) {
        cotizacion.setAplicada(true);
        cotizacion.setPorcentajeAjuste(porcentaje);
        cotizacion.setProductosAjustados(productos);
        cotizacion.setFechaAjuste(ahora);
        cotizacionDolarRepository.save(cotizacion);
    }

    private ActualizacionDolarResponse resultado(CotizacionDolar actual, Optional<CotizacionDolar> referencia,
                                                 ResultadoAjuste resultado, double porcentaje,
                                                 int productos, String mensaje) {
        return new ActualizacionDolarResponse(toResponse(actual, referencia), resultado,
                redondear(porcentaje), productos, mensaje);
    }

    private CotizacionDolarResponse toResponse(CotizacionDolar actual, Optional<CotizacionDolar> referencia) {
        Double valorReferencia = referencia.map(CotizacionDolar::getVenta).orElse(null);
        Double variacion = valorReferencia != null
                ? redondear(variacionPorcentaje(valorReferencia, actual.getVenta()))
                : null;
        return new CotizacionDolarResponse(
                actual.getCasa(),
                actual.getCompra(),
                actual.getVenta(),
                actual.getFechaActualizacion(),
                actual.getFechaConsulta(),
                valorReferencia,
                referencia.map(CotizacionDolar::getFechaAjuste).orElse(null),
                variacion,
                properties.getAjusteMinimoPorcentaje(),
                properties.getVariacionMaximaPorcentaje());
    }

    /** Cuánto se movió el dólar de anterior a nuevo, en % (positivo = subió). */
    static double variacionPorcentaje(double anterior, double nuevo) {
        return (nuevo / anterior - 1) * 100;
    }

    private static double aplicarFactor(double precio, BigDecimal factor) {
        return BigDecimal.valueOf(precio).multiply(factor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static String formatear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
