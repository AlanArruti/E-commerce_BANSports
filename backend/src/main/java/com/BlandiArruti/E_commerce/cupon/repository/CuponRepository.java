package com.BlandiArruti.E_commerce.cupon.repository;

import com.BlandiArruti.E_commerce.cupon.entity.Cupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CuponRepository extends JpaRepository<Cupon, Long> {

    Optional<Cupon> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<Cupon> findAllByOrderByFechaCreacionDescIdDesc();

    /**
     * Bloquea la fila hasta el fin de la transacción. Lo usa el checkout al aplicar el cupón:
     * dos pedidos a la vez con el último uso disponible no pueden llevárselo los dos.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cupon c where c.codigo = :codigo")
    Optional<Cupon> findByCodigoParaActualizar(@Param("codigo") String codigo);
}
