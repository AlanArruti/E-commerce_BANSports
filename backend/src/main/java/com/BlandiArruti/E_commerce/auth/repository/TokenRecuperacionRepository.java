package com.BlandiArruti.E_commerce.auth.repository;

import com.BlandiArruti.E_commerce.auth.entity.TokenRecuperacion;
import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

    Optional<TokenRecuperacion> findFirstByClienteOrderByFechaCreacionDesc(Cliente cliente);

    /** Al pedir un enlace nuevo, los anteriores dejan de servir. */
    @Modifying
    @Query("update TokenRecuperacion t set t.usado = true where t.cliente = :cliente and t.usado = false")
    int invalidarPendientes(@Param("cliente") Cliente cliente);
}
