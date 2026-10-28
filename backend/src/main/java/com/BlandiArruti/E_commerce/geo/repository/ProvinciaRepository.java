package com.BlandiArruti.E_commerce.geo.repository;

import com.BlandiArruti.E_commerce.geo.entity.Provincia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProvinciaRepository extends JpaRepository<Provincia, Long> {
    List<Provincia> findByPaisId(Long paisId);
}
