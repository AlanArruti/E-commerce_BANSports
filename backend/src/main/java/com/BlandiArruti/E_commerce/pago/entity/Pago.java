package com.BlandiArruti.E_commerce.pago.entity;

import com.BlandiArruti.E_commerce.enums.EstadoPago;
import com.BlandiArruti.E_commerce.enums.TipoFactura;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Un intento de pago de un pedido en una pasarela (MercadoPago u otra). Un pedido puede
 * tener varios: si el primero se rechaza, el cliente vuelve a intentar.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "pedido")

@Entity
@Table(name = "pagos",
       uniqueConstraints = @UniqueConstraint(name = "uk_pago_pasarela_id_externo",
                                             columnNames = {"pasarela", "id_externo"}))
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    /** Código de la pasarela que lo cobra (ver PasarelaPago#codigo). */
    @Column(name = "pasarela", nullable = false, length = 30)
    private String pasarela;

    /** Id del cobro en la pasarela (en MercadoPago, el id de la order). */
    @Column(name = "id_externo", length = 100)
    private String idExterno;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPago estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_factura", nullable = false, length = 1)
    private TipoFactura tipoFactura;

    /** Total del pedido al iniciar el pago. */
    @Column(name = "monto", nullable = false)
    private Double monto;

    /** Lo que informó la pasarela como pagado (null hasta que paga). */
    @Column(name = "monto_pagado")
    private Double montoPagado;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
