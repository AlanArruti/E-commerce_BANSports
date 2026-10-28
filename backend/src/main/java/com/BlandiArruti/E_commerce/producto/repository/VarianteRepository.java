package com.BlandiArruti.E_commerce.producto.repository;

import com.BlandiArruti.E_commerce.producto.entity.Variante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VarianteRepository extends JpaRepository<Variante, Long> {

    Optional<Variante> findByUuid(String uuid);

    Optional<Variante> findByIdAndActivoTrue(Long id);

    List<Variante> findByProductoIdAndActivoTrue(Long idProducto);
}
