package com.BlandiArruti.E_commerce.cotizacion.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Tarifa propia de respaldo para cuando la cotización de Correo Argentino no
 * está configurada o falla. Se cobra por provincia de destino; la fila con
 * provincia nula es la tarifa por defecto (así el checkout nunca queda trabado).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString

@Entity
@Table(name = "tarifas_envio")
public class TarifaEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tarifa")
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, updatable = false, length = 36)
    private String uuid;

    @PrePersist
    private void generarUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
    }

    /** Provincia de destino. null = tarifa por defecto para provincias sin tarifa específica. */
    @Column(name = "provincia", unique = true, length = 50)
    private String provincia;

    @Column(name = "precio", nullable = false)
    private Double precio;

    @Column(name = "plazo_minimo_dias", nullable = false)
    private Integer plazoMinimoDias;

    @Column(name = "plazo_maximo_dias", nullable = false)
    private Integer plazoMaximoDias;
}
