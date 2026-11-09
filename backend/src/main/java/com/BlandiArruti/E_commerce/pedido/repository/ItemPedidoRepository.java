package com.BlandiArruti.E_commerce.pedido.repository;

import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.pedido.entity.ItemPedido;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {

    List<ItemPedido> findByPedidoId(Long idPedido);

    /**
     * Productos más vendidos (por unidades) en pedidos cobrados del período.
     * Una fila por producto: [idProducto, nombre, unidades, total facturado sin envío].
     */
    @Query("SELECT i.producto.id, i.producto.nombre, SUM(i.cantidad), SUM(i.cantidad * i.precioProducto) " +
           "FROM ItemPedido i " +
           "WHERE i.pedido.estadoPedido IN :estados " +
           "AND i.pedido.fechaCreacion >= :desde AND i.pedido.fechaCreacion < :hasta " +
           "GROUP BY i.producto.id, i.producto.nombre " +
           "ORDER BY SUM(i.cantidad) DESC, i.producto.nombre")
    List<Object[]> masVendidosEntre(@Param("estados") List<EstadoPedido> estados,
                                    @Param("desde") LocalDateTime desde,
                                    @Param("hasta") LocalDateTime hasta,
                                    Pageable pageable);
}
