package com.BlandiArruti.E_commerce.pedido.repository;

import com.BlandiArruti.E_commerce.pedido.entity.ItemPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {

    List<ItemPedido> findByPedidoId(Long idPedido);
}
