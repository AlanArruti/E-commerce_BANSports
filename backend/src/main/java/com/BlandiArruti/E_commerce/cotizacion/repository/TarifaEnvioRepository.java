package com.BlandiArruti.E_commerce.cotizacion.repository;

import com.BlandiArruti.E_commerce.cotizacion.entity.TarifaEnvio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TarifaEnvioRepository extends JpaRepository<TarifaEnvio, Long> {

    Optional<TarifaEnvio> findByProvinciaIgnoreCase(String provincia);

    Optional<TarifaEnvio> findByProvinciaIsNull();
}
