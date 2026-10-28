package com.BlandiArruti.E_commerce.factura.repository;

import com.BlandiArruti.E_commerce.enums.TipoFactura;
import com.BlandiArruti.E_commerce.factura.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    Optional<Factura> findByUuid(String uuid);

    Optional<Factura> findByPedidoId(Long idPedido);

    List<Factura> findByTipoFactura(TipoFactura tipo);

    List<Factura> findByPedidoClienteId(Long idCliente);
}
