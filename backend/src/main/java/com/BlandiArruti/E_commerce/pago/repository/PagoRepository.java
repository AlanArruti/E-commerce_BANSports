package com.BlandiArruti.E_commerce.pago.repository;

import com.BlandiArruti.E_commerce.pago.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    /** Lo usa el webhook: la pasarela avisa con su propio id. */
    Optional<Pago> findByPasarelaAndIdExterno(String pasarela, String idExterno);

    List<Pago> findByPedidoIdOrderByFechaCreacionDesc(Long idPedido);
}
