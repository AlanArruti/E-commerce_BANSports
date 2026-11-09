package com.BlandiArruti.E_commerce.admin.service;

import com.BlandiArruti.E_commerce.admin.dto.response.MetricasAdminResponse;
import com.BlandiArruti.E_commerce.admin.dto.response.MetricasAdminResponse.PedidosPorEstado;
import com.BlandiArruti.E_commerce.admin.dto.response.MetricasAdminResponse.ProductoMasVendido;
import com.BlandiArruti.E_commerce.admin.dto.response.MetricasAdminResponse.VentaDiaria;
import com.BlandiArruti.E_commerce.admin.dto.response.ResumenAdminResponse;
import com.BlandiArruti.E_commerce.admin.dto.response.VarianteStockBajoResponse;
import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.factura.repository.FacturaRepository;
import com.BlandiArruti.E_commerce.pedido.repository.ItemPedidoRepository;
import com.BlandiArruti.E_commerce.pedido.repository.PedidoRepository;
import com.BlandiArruti.E_commerce.producto.entity.Variante;
import com.BlandiArruti.E_commerce.producto.repository.ProductoRepository;
import com.BlandiArruti.E_commerce.producto.repository.VarianteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminResumenService {

    /** Umbral de "stock bajo" (coincide con el STOCK_BAJO del front). */
    private static final int STOCK_BAJO = 5;

    /** Estados que ya pagaron: sus facturas son las "ventas". */
    private static final List<EstadoPedido> ESTADOS_COBRADOS =
            List.of(EstadoPedido.PAGADO, EstadoPedido.DESPACHADO, EstadoPedido.ENTREGADO);

    /** Períodos que acepta {@link #metricas(int)}. */
    public static final List<Integer> DIAS_PERMITIDOS = List.of(7, 30, 90);

    private static final int CANTIDAD_MAS_VENDIDOS = 5;

    private final PedidoRepository pedidoRepository;
    private final VarianteRepository varianteRepository;
    private final ProductoRepository productoRepository;
    private final FacturaRepository facturaRepository;
    private final ItemPedidoRepository itemPedidoRepository;

    public ResumenAdminResponse resumen() {
        YearMonth mes = YearMonth.now();
        LocalDateTime desde = mes.atDay(1).atStartOfDay();
        LocalDateTime hasta = mes.plusMonths(1).atDay(1).atStartOfDay();

        double ventasMes = facturaRepository.sumarVentasEntre(ESTADOS_COBRADOS, desde, hasta);

        return new ResumenAdminResponse(
                ventasMes,
                pedidoRepository.countByEstadoPedido(EstadoPedido.PENDIENTE_PAGO),
                pedidoRepository.countByEstadoPedido(EstadoPedido.EN_PREPARACION),
                pedidoRepository.countByEstadoPedido(EstadoPedido.PAGADO),
                varianteRepository.countByActivoTrueAndStock(0),
                varianteRepository.countByActivoTrueAndStockBetween(1, STOCK_BAJO),
                productoRepository.countOfertasActivas()
        );
    }

    /**
     * Ventas día por día y productos más vendidos de los últimos {@code dias} días
     * (hoy incluido), los totales del período anterior para comparar, y cuántos
     * pedidos hay hoy en cada estado.
     */
    public MetricasAdminResponse metricas(int dias) {
        if (!DIAS_PERMITIDOS.contains(dias)) {
            throw new EcommerceException("El período tiene que ser de 7, 30 o 90 días");
        }
        LocalDate hoy = LocalDate.now();
        LocalDate primerDia = hoy.minusDays(dias - 1L);
        LocalDateTime desde = primerDia.atStartOfDay();
        LocalDateTime hasta = hoy.plusDays(1).atStartOfDay();
        LocalDateTime desdeAnterior = primerDia.minusDays(dias).atStartOfDay();

        List<VentaDiaria> ventasPorDia = ventasPorDia(primerDia, dias,
                facturaRepository.ventasEntre(ESTADOS_COBRADOS, desde, hasta));

        List<ProductoMasVendido> masVendidos = itemPedidoRepository
                .masVendidosEntre(ESTADOS_COBRADOS, desde, hasta, PageRequest.of(0, CANTIDAD_MAS_VENDIDOS))
                .stream()
                .map(fila -> new ProductoMasVendido(
                        (Long) fila[0],
                        (String) fila[1],
                        ((Number) fila[2]).longValue(),
                        ((Number) fila[3]).doubleValue()))
                .toList();

        return new MetricasAdminResponse(
                dias,
                ventasPorDia.stream().mapToDouble(VentaDiaria::total).sum(),
                facturaRepository.sumarVentasEntre(ESTADOS_COBRADOS, desdeAnterior, desde),
                ventasPorDia.stream().mapToLong(VentaDiaria::pedidos).sum(),
                facturaRepository.contarVentasEntre(ESTADOS_COBRADOS, desdeAnterior, desde),
                ventasPorDia,
                pedidosPorEstado(),
                masVendidos
        );
    }

    public List<VarianteStockBajoResponse> variantesStockBajo() {
        return varianteRepository.findStockBajo(STOCK_BAJO).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Agrupa las ventas [fecha, total] por día y completa con ceros los días sin ventas. */
    private List<VentaDiaria> ventasPorDia(LocalDate primerDia, int dias, List<Object[]> ventas) {
        Map<LocalDate, Double> totales = new LinkedHashMap<>();
        Map<LocalDate, Long> pedidos = new LinkedHashMap<>();
        for (int i = 0; i < dias; i++) {
            totales.put(primerDia.plusDays(i), 0.0);
            pedidos.put(primerDia.plusDays(i), 0L);
        }
        for (Object[] venta : ventas) {
            LocalDate dia = ((LocalDateTime) venta[0]).toLocalDate();
            if (totales.containsKey(dia)) {
                totales.merge(dia, ((Number) venta[1]).doubleValue(), Double::sum);
                pedidos.merge(dia, 1L, Long::sum);
            }
        }
        return totales.keySet().stream()
                .map(dia -> new VentaDiaria(dia, totales.get(dia), pedidos.get(dia)))
                .toList();
    }

    private List<PedidosPorEstado> pedidosPorEstado() {
        Map<EstadoPedido, Long> cantidades = new EnumMap<>(EstadoPedido.class);
        for (Object[] fila : pedidoRepository.contarPorEstado()) {
            cantidades.put((EstadoPedido) fila[0], ((Number) fila[1]).longValue());
        }
        return Arrays.stream(EstadoPedido.values())
                .map(estado -> new PedidosPorEstado(estado.name(), cantidades.getOrDefault(estado, 0L)))
                .toList();
    }

    private VarianteStockBajoResponse toResponse(Variante variante) {
        return new VarianteStockBajoResponse(
                variante.getId(),
                variante.getProducto().getId(),
                variante.getProducto().getNombre(),
                variante.getAtributos(),
                variante.getStock()
        );
    }
}
