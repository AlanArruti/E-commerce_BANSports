package com.BlandiArruti.E_commerce.notificacion.repository;

import com.BlandiArruti.E_commerce.notificacion.entity.Notificacion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByClienteIdOrderByFechaCreacionDescIdDesc(Long idCliente, Pageable pageable);

    long countByClienteIdAndLeidaFalse(Long idCliente);

    Optional<Notificacion> findByIdAndClienteId(Long id, Long idCliente);

    @Modifying
    @Query("update Notificacion n set n.leida = true where n.cliente.id = :idCliente and n.leida = false")
    int marcarTodasComoLeidas(@Param("idCliente") Long idCliente);

    /**
     * Una oferta le llega a todos los clientes activos. Es un solo INSERT ... SELECT para no
     * traer a memoria a todos los clientes ni hacer un insert por cada uno.
     */
    @Modifying
    @Query(value = """
            INSERT INTO notificaciones (id_cliente, tipo, titulo, mensaje, id_referencia, leida, fecha_creacion)
            SELECT c.id, :tipo, :titulo, :mensaje, :idReferencia, false, :fecha
            FROM clientes c
            WHERE c.activo = true
            """, nativeQuery = true)
    int crearParaClientesActivos(@Param("tipo") String tipo,
                                 @Param("titulo") String titulo,
                                 @Param("mensaje") String mensaje,
                                 @Param("idReferencia") Long idReferencia,
                                 @Param("fecha") LocalDateTime fecha);
}
