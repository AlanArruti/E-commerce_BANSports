package com.BlandiArruti.E_commerce.carrito.repository;

import com.BlandiArruti.E_commerce.carrito.entity.ItemCarrito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, Long> {

    Optional<ItemCarrito> findByCarritoIdAndVarianteId(Long idCarrito, Long idVariante);

    Optional<ItemCarrito> findByCarritoIdAndProductoIdAndVarianteIsNull(Long idCarrito, Long idProducto);

    Optional<ItemCarrito> findByIdAndCarritoId(Long idItem, Long idCarrito);
}
