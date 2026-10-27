package com.BlandiArruti.E_commerce.producto.entity;

import com.BlandiArruti.E_commerce.categoria.entity.Categoria;
import jakarta.persistence.*;
import lombok.*;

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
    }
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;
    @Column(name = "descripcion", nullable = false, length = 250)
    private String descripcion;
    @Column(name = "precio", nullable = false)
    private Double precio;
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

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

