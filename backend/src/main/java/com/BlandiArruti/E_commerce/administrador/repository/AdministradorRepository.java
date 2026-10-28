package com.BlandiArruti.E_commerce.administrador.repository;

import com.BlandiArruti.E_commerce.administrador.entity.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {

    Optional<Administrador> findByUuid(String uuid);

    Optional<Administrador> findByUsername(String username);
}
