package com.BlandiArruti.E_commerce.producto.repository;

import com.BlandiArruti.E_commerce.producto.entity.Variante;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VarianteRepository extends JpaRepository<Variante, Long> {

    Optional<Variante> findByUuid(String uuid);

    Optional<Variante> findByIdAndActivoTrue(Long id);

    /** Lee la variante bloqueando la fila (SELECT ... FOR UPDATE), para descontar stock sin carreras. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Variante v WHERE v.id = :id")
    Optional<Variante> findByIdParaActualizar(@Param("id") Long id);

    List<Variante> findByProductoIdAndActivoTrue(Long idProducto);

    long countByActivoTrueAndStock(int stock);

    long countByActivoTrueAndStockBetween(int min, int max);

    @Query("SELECT v FROM Variante v JOIN FETCH v.producto p " +
           "WHERE v.activo = true AND v.stock <= :max " +
           "ORDER BY v.stock ASC, v.id ASC")
    List<Variante> findStockBajo(@Param("max") int max);
}
