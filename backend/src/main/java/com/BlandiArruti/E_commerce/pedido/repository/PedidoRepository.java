package com.BlandiArruti.E_commerce.pedido.repository;

import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByUuid(String uuid);

    List<Pedido> findByClienteId(Long idCliente);

    List<Pedido> findByEstadoPedido(EstadoPedido estado);

    List<Pedido> findByClienteIdAndEstadoPedido(Long idCliente, EstadoPedido estado);

    Page<Pedido> findAll(Pageable pageable);

    Page<Pedido> findByClienteId(Long idCliente, Pageable pageable);

    Page<Pedido> findByEstadoPedido(EstadoPedido estado, Pageable pageable);

    Page<Pedido> findByClienteIdAndEstadoPedido(Long idCliente, EstadoPedido estado, Pageable pageable);
}
