package com.BlandiArruti.E_commerce.resena.repository;

import com.BlandiArruti.E_commerce.resena.entity.Resena;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ResenaRepository extends JpaRepository<Resena, Long> {

    Page<Resena> findByProductoId(Long idProducto, Pageable pageable);

    Optional<Resena> findByProductoIdAndClienteId(Long idProducto, Long idCliente);

    boolean existsByProductoIdAndClienteId(Long idProducto, Long idCliente);

    Optional<Resena> findByIdAndProductoId(Long id, Long idProducto);

    long countByProductoId(Long idProducto);

    /** null si el producto no tiene reseñas. */
    @Query("SELECT AVG(r.estrellas) FROM Resena r WHERE r.producto.id = :idProducto")
    Double promedioDeProducto(@Param("idProducto") Long idProducto);
}
