package com.BlandiArruti.E_commerce.pedido.repository;

import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.pedido.entity.ItemPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {

    List<ItemPedido> findByPedidoId(Long idPedido);

    /** Si el cliente tiene ese producto en algún pedido que esté en uno de los estados indicados. */
    @Query("SELECT COUNT(i) > 0 FROM ItemPedido i " +
           "WHERE i.pedido.cliente.id = :idCliente " +
           "AND i.producto.id = :idProducto " +
           "AND i.pedido.estadoPedido IN :estados")
    boolean clienteComproProducto(@Param("idCliente") Long idCliente,
                                  @Param("idProducto") Long idProducto,
                                  @Param("estados") Collection<EstadoPedido> estados);
}
