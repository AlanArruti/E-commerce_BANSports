package com.BlandiArruti.E_commerce.geo.repository;

import com.BlandiArruti.E_commerce.geo.entity.Pais;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaisRepository extends JpaRepository<Pais, Long> {
    Optional<Pais> findByNombreIgnoreCase(String nombre);
}
