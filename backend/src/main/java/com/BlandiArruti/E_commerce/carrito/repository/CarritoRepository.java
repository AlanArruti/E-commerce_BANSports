package com.BlandiArruti.E_commerce.carrito.repository;

import com.BlandiArruti.E_commerce.carrito.entity.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    Optional<Carrito> findByClienteId(Long idCliente);
}
