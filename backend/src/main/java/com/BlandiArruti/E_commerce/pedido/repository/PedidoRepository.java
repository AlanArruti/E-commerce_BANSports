package com.BlandiArruti.E_commerce.pedido.repository;

import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByUuid(String uuid);

    /**
     * Bloquea la fila hasta el fin de la transacción. Lo usa la confirmación de MercadoPago:
     * el webhook y la página de vuelta pueden llegar a la vez, y sin el bloqueo los dos
     * verían el pedido sin pagar y descontarían el stock dos veces.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> findByIdParaActualizar(@Param("id") Long id);

    List<Pedido> findByClienteId(Long idCliente);

    List<Pedido> findByEstadoPedido(EstadoPedido estado);

    List<Pedido> findByClienteIdAndEstadoPedido(Long idCliente, EstadoPedido estado);

    Page<Pedido> findAll(Pageable pageable);

    Page<Pedido> findByClienteId(Long idCliente, Pageable pageable);

    Page<Pedido> findByEstadoPedido(EstadoPedido estado, Pageable pageable);

    Page<Pedido> findByClienteIdAndEstadoPedido(Long idCliente, EstadoPedido estado, Pageable pageable);
}
