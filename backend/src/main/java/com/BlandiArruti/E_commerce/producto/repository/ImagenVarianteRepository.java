package com.BlandiArruti.E_commerce.producto.repository;

import com.BlandiArruti.E_commerce.producto.entity.ImagenVariante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImagenVarianteRepository extends JpaRepository<ImagenVariante, Long> {

    Optional<ImagenVariante> findByVarianteId(Long idVariante);
}
