package com.BlandiArruti.E_commerce.resena.entity;

import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cliente", "producto"})

@Entity
@Table(name = "resenas",
        uniqueConstraints = @UniqueConstraint(name = "uk_resena_cliente_producto",
                columnNames = {"id_cliente", "id_producto"}))
public class Resena {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resena")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    /** De 1 a 5. */
    @Column(name = "estrellas", nullable = false)
    private Integer estrellas;

    /** Opcional: se puede puntuar sin escribir nada. */
    @Column(name = "comentario", length = 500)
    private String comentario;

    @CreationTimestamp
    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;
}
