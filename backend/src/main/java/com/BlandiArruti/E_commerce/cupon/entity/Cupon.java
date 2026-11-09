package com.BlandiArruti.E_commerce.cupon.entity;

import com.BlandiArruti.E_commerce.enums.TipoCupon;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cupón de descuento sobre el subtotal de productos de un pedido.
 * A diferencia de la oferta (SCRUM-124), que va por producto, el cupón lo carga el
 * cliente en el checkout y se aplica una sola vez al pedido entero, sin el envío.
 *
 * Es una tabla nueva: los DEFAULT de las columnas no hacen falta para ddl-auto,
 * pero se dejan igual por si la tabla ya existe con filas en otra base.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString

@Entity
@Table(name = "cupones")
public class Cupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cupon")
    private Long id;

    /** Siempre en mayúsculas: el cliente lo puede escribir como quiera. */
    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoCupon tipo;

    /** Porcentaje (1 a 100) o monto en pesos, según el tipo. */
    @Column(name = "valor", nullable = false)
    private Double valor;

    /** Primer día en que se puede usar. Null = desde que se crea. */
    @Column(name = "vigente_desde")
    private LocalDate vigenteDesde;

    /** Último día en que se puede usar (inclusive). Null = sin vencimiento. */
    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    /** Subtotal de productos mínimo para poder usarlo. 0 = sin mínimo. */
    @Column(name = "monto_minimo", nullable = false, columnDefinition = "double precision default 0 not null")
    @Builder.Default
    private Double montoMinimo = 0.0;

    /** Cuántos pedidos lo pueden usar en total. Null = sin límite. */
    @Column(name = "usos_maximos")
    private Integer usosMaximos;

    /** Pedidos que lo usaron (se descuenta si el pedido se cancela). */
    @Column(name = "usos", nullable = false, columnDefinition = "integer default 0 not null")
    @Builder.Default
    private int usos = 0;

    /** Baja lógica: los pedidos viejos lo siguen referenciando. */
    @Column(name = "activo", nullable = false, columnDefinition = "boolean default true not null")
    @Builder.Default
    private boolean activo = true;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false,
            columnDefinition = "timestamp default now() not null")
    private LocalDateTime fechaCreacion;

    public boolean estaAgotado() {
        return usosMaximos != null && usos >= usosMaximos;
    }
}
