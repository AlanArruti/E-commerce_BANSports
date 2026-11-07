package com.BlandiArruti.E_commerce.admin.service;

import com.BlandiArruti.E_commerce.admin.dto.response.ResumenAdminResponse;
import com.BlandiArruti.E_commerce.admin.dto.response.VarianteStockBajoResponse;
import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.factura.repository.FacturaRepository;
import com.BlandiArruti.E_commerce.pedido.repository.PedidoRepository;
import com.BlandiArruti.E_commerce.producto.entity.Variante;
import com.BlandiArruti.E_commerce.producto.repository.ProductoRepository;
import com.BlandiArruti.E_commerce.producto.repository.VarianteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminResumenService {

    /** Umbral de "stock bajo" (coincide con el STOCK_BAJO del front). */
    private static final int STOCK_BAJO = 5;

    /** Estados que ya pagaron: sus facturas son las "ventas". */
    private static final List<EstadoPedido> ESTADOS_COBRADOS =
            List.of(EstadoPedido.PAGADO, EstadoPedido.DESPACHADO, EstadoPedido.ENTREGADO);

    private final PedidoRepository pedidoRepository;
    private final VarianteRepository varianteRepository;
    private final ProductoRepository productoRepository;
    private final FacturaRepository facturaRepository;

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

    public List<VarianteStockBajoResponse> variantesStockBajo() {
        return varianteRepository.findStockBajo(STOCK_BAJO).stream()
                .map(this::toResponse)
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
