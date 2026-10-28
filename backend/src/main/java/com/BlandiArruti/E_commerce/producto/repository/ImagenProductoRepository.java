package com.BlandiArruti.E_commerce.producto.repository;

import com.BlandiArruti.E_commerce.producto.entity.ImagenProducto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImagenProductoRepository extends JpaRepository<ImagenProducto, Long> {

    Optional<ImagenProducto> findByProductoId(Long idProducto);

    void deleteByProductoId(Long idProducto);
}
