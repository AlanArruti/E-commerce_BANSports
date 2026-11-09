package com.BlandiArruti.E_commerce.cliente.repository;

import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByUuid(String uuid);

    Optional<Cliente> findByEmail(String email);

    Optional<Cliente> findByIdAndActivoTrue(Long id);

    List<Cliente> findAllByActivoTrue();

    Page<Cliente> findAllByActivoTrue(Pageable pageable);

    /**
     * Clientes activos cuyo nombre, apellido, "nombre apellido" o email contienen el patrón.
     * El patrón llega en minúsculas, con % alrededor y los comodines del usuario escapados con \.
     */
    @Query("SELECT c FROM Cliente c WHERE c.activo = true AND (" +
           "LOWER(c.nombre) LIKE :patron ESCAPE '\\' OR " +
           "LOWER(c.apellido) LIKE :patron ESCAPE '\\' OR " +
           "LOWER(CONCAT(c.nombre, ' ', c.apellido)) LIKE :patron ESCAPE '\\' OR " +
           "LOWER(c.email) LIKE :patron ESCAPE '\\')")
    Page<Cliente> buscarActivos(@Param("patron") String patron, Pageable pageable);

    boolean existsByEmail(String email);

    boolean existsByDni(String dni);
}
