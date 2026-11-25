package com.BlandiArruti.E_commerce.cliente.repository;

import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.BlandiArruti.E_commerce.cliente.util.Emails;
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

    Optional<Cliente> findFirstByEmailIgnoreCaseOrderByIdAsc(String email);

    /** Cuentas de Gmail cuyo usuario, sin puntos ni "+etiqueta", es el dado (ver {@link Emails#usuarioGmail}). */
    @Query(value = """
            SELECT * FROM clientes c
            WHERE split_part(lower(c.email), '@', 2) IN ('gmail.com', 'googlemail.com')
              AND replace(split_part(split_part(lower(c.email), '@', 1), '+', 1), '.', '') = :usuario
            ORDER BY c.id
            LIMIT 1
            """, nativeQuery = true)
    Optional<Cliente> findGmailPorUsuario(@Param("usuario") String usuario);

    /**
     * La cuenta que corresponde a ese email aunque esté escrito distinto: sin importar mayúsculas y,
     * en Gmail, sin importar puntos ni "+etiqueta". La usan el registro, el login y el login social.
     */
    default Optional<Cliente> buscarPorEmailEquivalente(String email) {
        String normal = Emails.normalizar(email);
        if (normal == null || normal.isEmpty()) {
            return Optional.empty();
        }
        Optional<Cliente> exacto = findFirstByEmailIgnoreCaseOrderByIdAsc(normal);
        if (exacto.isPresent()) {
            return exacto;
        }
        String usuarioGmail = Emails.usuarioGmail(normal);
        return usuarioGmail == null ? Optional.empty() : findGmailPorUsuario(usuarioGmail);
    }

    boolean existsByDni(String dni);
}
