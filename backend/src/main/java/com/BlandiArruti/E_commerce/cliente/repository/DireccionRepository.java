package com.BlandiArruti.E_commerce.cliente.repository;

import com.BlandiArruti.E_commerce.cliente.entity.Direccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DireccionRepository extends JpaRepository<Direccion, Long> {

    Optional<Direccion> findByUuid(String uuid);
}
