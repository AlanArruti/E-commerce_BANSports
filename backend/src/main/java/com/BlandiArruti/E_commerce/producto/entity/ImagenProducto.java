package com.BlandiArruti.E_commerce.producto.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Imagen de un producto guardada en la base (bytea). Vive en su propia tabla
 * para que el listado del catálogo nunca arrastre los bytes: se sirve aparte
 * por GET /api/v1/producto/{id}/imagen.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"producto", "datos"})

@Entity
@Table(name = "imagenes_producto")
public class ImagenProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imagen")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false, unique = true)
    private Producto producto;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Lob
    @Column(name = "datos", nullable = false)
    private byte[] datos;
}
