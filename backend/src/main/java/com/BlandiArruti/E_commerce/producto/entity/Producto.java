package com.BlandiArruti.E_commerce.producto.entity;

import com.BlandiArruti.E_commerce.categoria.entity.Categoria;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"categoria", "variantes"})

@Entity
@Table(name = "productos")
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, updatable = false, length = 36)
    private String uuid;

    @PrePersist
    private void generarUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
        recalcularPrecioFinal();
    }
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;
    @Column(name = "descripcion", nullable = false, length = 250)
    private String descripcion;
    @Column(name = "precio", nullable = false)
    private Double precio;
    /** Descuento porcentual (20 = 20% off). null o 0 significa sin oferta. */
    @Column(name = "descuento", columnDefinition = "integer default 0")
    @Builder.Default
    private Integer descuento = 0;
    /** Precio ya con el descuento aplicado. Lo mantiene calcularPrecioFinal(). */
    @Column(name = "precio_final")
    private Double precioFinal;
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    /** Precio que paga el cliente: el de oferta si hay, o el base. */
    public double getPrecioConDescuento() {
        if (precio == null) {
            return 0;
        }
        int porcentaje = descuento == null ? 0 : descuento;
        if (porcentaje <= 0 || porcentaje >= 100) {
            return redondear(precio);
        }
        return BigDecimal.valueOf(precio)
                .multiply(BigDecimal.valueOf(100 - porcentaje))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    @PreUpdate
    private void calcularPrecioFinal() {
        recalcularPrecioFinal();
    }

    /** Vuelve a calcular el precio final después de cambiar precio o descuento. */
    public void recalcularPrecioFinal() {
        this.precioFinal = getPrecioConDescuento();
    }

    private static double redondear(Double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Bandera desnormalizada: evita consultar la tabla de imágenes por cada
     * producto del catálogo solo para saber si hay que mostrar una foto.
     * La mantiene ProductoService al subir o borrar la imagen.
     */
    // El DEFAULT permite que ddl-auto=update agregue la columna sobre una tabla
    // que ya tiene filas cargadas.
    @Column(name = "tiene_imagen", nullable = false,
            columnDefinition = "boolean default false not null")
    @Builder.Default
    private boolean tieneImagen = false;

    @ManyToOne
    @JoinColumn(name = "id_categoria")
    private Categoria categoria;
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Variante> variantes = new ArrayList<>();
}

