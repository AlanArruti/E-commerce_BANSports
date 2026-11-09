package com.BlandiArruti.E_commerce.factura.repository;

import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.enums.TipoFactura;
import com.BlandiArruti.E_commerce.factura.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    Optional<Factura> findByUuid(String uuid);

    Optional<Factura> findByPedidoId(Long idPedido);

    List<Factura> findByTipoFactura(TipoFactura tipo);

    List<Factura> findByPedidoClienteId(Long idCliente);

    @Query("SELECT COALESCE(SUM(f.precioTotal), 0.0) FROM Factura f " +
           "WHERE f.pedido.estadoPedido IN :estados " +
           "AND f.pedido.fechaCreacion >= :desde AND f.pedido.fechaCreacion < :hasta")
    Double sumarVentasEntre(@Param("estados") List<EstadoPedido> estados,
                            @Param("desde") LocalDateTime desde,
                            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COUNT(f) FROM Factura f " +
           "WHERE f.pedido.estadoPedido IN :estados " +
           "AND f.pedido.fechaCreacion >= :desde AND f.pedido.fechaCreacion < :hasta")
    long contarVentasEntre(@Param("estados") List<EstadoPedido> estados,
                           @Param("desde") LocalDateTime desde,
                           @Param("hasta") LocalDateTime hasta);

    /** Una fila por venta: [fechaCreacion del pedido, precioTotal de la factura]. */
    @Query("SELECT f.pedido.fechaCreacion, f.precioTotal FROM Factura f " +
           "WHERE f.pedido.estadoPedido IN :estados " +
           "AND f.pedido.fechaCreacion >= :desde AND f.pedido.fechaCreacion < :hasta")
    List<Object[]> ventasEntre(@Param("estados") List<EstadoPedido> estados,
                               @Param("desde") LocalDateTime desde,
                               @Param("hasta") LocalDateTime hasta);
}
