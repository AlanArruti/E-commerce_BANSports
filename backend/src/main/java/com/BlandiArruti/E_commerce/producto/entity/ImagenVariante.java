package com.BlandiArruti.E_commerce.producto.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Foto propia de una variante (ej. la Whey de chocolate y la de vainilla). Igual que
 * ImagenProducto: tabla aparte para que listar variantes no arrastre los bytes.
 * Si la variante no tiene foto, el front muestra la del producto.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"variante", "datos"})

@Entity
@Table(name = "imagenes_variante")
public class ImagenVariante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imagen")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_variante", nullable = false, unique = true)
    private Variante variante;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Lob
    @Column(name = "datos", nullable = false)
    private byte[] datos;
}
