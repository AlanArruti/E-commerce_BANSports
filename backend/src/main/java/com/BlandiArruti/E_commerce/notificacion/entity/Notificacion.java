package com.BlandiArruti.E_commerce.notificacion.entity;

import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.enums.TipoNotificacion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Aviso que el cliente ve en la página (campanita del header). Los mails siguen saliendo
 * aparte por NotificacionService: esto es el historial dentro del sitio (RF02).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cliente"})

@Entity
@Table(name = "notificaciones",
        indexes = @Index(name = "idx_notificacion_cliente_fecha", columnList = "id_cliente, fecha_creacion"))
public class Notificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoNotificacion tipo;

    @Column(name = "titulo", nullable = false, length = 80)
    private String titulo;

    @Column(name = "mensaje", nullable = false, length = 300)
    private String mensaje;

    /** Id del pedido o del producto, según el tipo. El front arma el enlace con esto. */
    @Column(name = "id_referencia")
    private Long idReferencia;

    @Builder.Default
    @Column(name = "leida", nullable = false, columnDefinition = "boolean default false")
    private boolean leida = false;

    // La fecha la pone el servicio (y no @CreationTimestamp) porque las ofertas se insertan
    // de a muchas con una consulta nativa que no pasa por Hibernate.
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
}
