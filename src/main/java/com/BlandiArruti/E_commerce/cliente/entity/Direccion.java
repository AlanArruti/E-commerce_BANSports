package com.BlandiArruti.E_commerce.cliente.entity;

import com.BlandiArruti.E_commerce.geo.entity.Ciudad;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"ciudad", "cliente"}) // evito loops en toString por relaciones bidireccionales

@Entity
@Table(name = "direcciones")
public class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_direccion") // HAY QUE DARLE NOMBRE AL ID
    private Long id; // Long porque el YAML usa format: int64

    @Column(name = "uuid", nullable = false, unique = true, updatable = false, length = 36)
    private String uuid;

    @PrePersist
    private void generarUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
    }

    @Column(name = "nombre_calle", nullable = false, length = 100) // NO PUEDE SER NULLO NI MAYOR A 100CRT
    private String nombreCalle;

    @Column(name = "numero_calle", nullable = false)
    private Integer numeroCalle;

    // El DEFAULT permite que ddl-auto=update agregue la columna sobre una tabla
    // que ya tiene direcciones cargadas. Sin él, el ALTER falla en silencio y
    // toda consulta sobre direcciones termina en 500.
    @Column(name = "codigo_postal", nullable = false, length = 8,
            columnDefinition = "varchar(8) default '0000' not null")
    private String codigoPostal;

    // -------- RELACIONES --------
    // Muchas direcciones pertenecen a UNA ciudad.
    // Esta clase es el LADO DUEÑO: acá vive la FK "id_ciudad".
    // LAZY para no traer toda la cadena Ciudad -> Provincia -> Pais sin querer.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ciudad", nullable = false)
    private Ciudad ciudad;

    // En Cliente, el inverso será: @OneToMany(mappedBy = "cliente")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;
}

