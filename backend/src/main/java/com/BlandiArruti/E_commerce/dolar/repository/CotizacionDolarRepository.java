package com.BlandiArruti.E_commerce.dolar.repository;

import com.BlandiArruti.E_commerce.dolar.entity.CotizacionDolar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CotizacionDolarRepository extends JpaRepository<CotizacionDolar, Long> {

    /** La última cotización leída. */
    Optional<CotizacionDolar> findFirstByOrderByIdDesc();

    /** La cotización con la que se ajustaron los precios por última vez (la referencia). */
    Optional<CotizacionDolar> findFirstByAplicadaTrueOrderByIdDesc();

    /** Historial de ajustes de precios, del más nuevo al más viejo. */
    List<CotizacionDolar> findTop50ByAplicadaTrueOrderByIdDesc();
}
