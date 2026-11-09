package com.BlandiArruti.E_commerce.admin.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Métricas del panel para los últimos N días (hoy incluido), comparadas con los
 * N días anteriores. Las ventas son las facturas de pedidos ya cobrados.
 */
public record MetricasAdminResponse(
        int dias,
        double ventasPeriodo,
        double ventasPeriodoAnterior,
        long pedidosPeriodo,
        long pedidosPeriodoAnterior,
        List<VentaDiaria> ventasPorDia,
        List<PedidosPorEstado> pedidosPorEstado,
        List<ProductoMasVendido> masVendidos
) {

    /** Un día del período; los días sin ventas vienen en cero. */
    public record VentaDiaria(LocalDate fecha, double total, long pedidos) {}

    /** Todos los estados, en el orden del flujo del pedido, aunque tengan cero. */
    public record PedidosPorEstado(String estado, long cantidad) {}

    /** Total facturado por el producto, sin el envío. */
    public record ProductoMasVendido(Long idProducto, String nombre, long unidades, double total) {}
}
