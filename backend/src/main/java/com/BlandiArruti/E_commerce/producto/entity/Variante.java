package com.BlandiArruti.E_commerce.producto.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"producto", "atributos"})

@Entity
@Table(name = "variantes")
public class Variante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_variante")
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, updatable = false, length = 36)
    private String uuid;

    @PrePersist
    private void generarUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "atributos", columnDefinition = "jsonb")
    private Map<String, String> atributos;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    /**
     * Precio propio de la variante, sin descuento (ej. la de 2 kg cuesta más que la de 1 kg).
     * null = usa el precio del producto. Ver Producto#precioUnitario.
     */
    @Column(name = "precio")
    private Double precio;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    /**
     * Bandera desnormalizada, como Producto.tieneImagen: dice si hay fila en imagenes_variante
     * sin consultarla. La mantiene ProductoService. Con DEFAULT para que ddl-auto=update pueda
     * agregar la columna sobre una tabla con filas.
     */
    @Column(name = "tiene_imagen", nullable = false,
            columnDefinition = "boolean default false not null")
    @Builder.Default
    private boolean tieneImagen = false;
}
