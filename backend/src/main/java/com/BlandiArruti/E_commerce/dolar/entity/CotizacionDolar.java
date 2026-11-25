package com.BlandiArruti.E_commerce.dolar.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Una cotización del dólar leída de DolarApi. Se guarda una fila cada vez que el valor
 * cambia (si no cambia, solo se actualiza fechaConsulta de la última).
 *
 * La fila con aplicada = true más reciente es la "cotización de referencia": la que tienen
 * incorporada los precios actuales. Cada ajuste compara contra ella.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString

@Entity
@Table(name = "cotizaciones_dolar")
public class CotizacionDolar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cotizacion_dolar")
    private Long id;

    /** Tipo de dólar: oficial, blue, bolsa... */
    @Column(name = "casa", nullable = false, length = 30)
    private String casa;

    @Column(name = "compra")
    private Double compra;

    @Column(name = "venta", nullable = false)
    private Double venta;

    /** Cuándo actualizó el valor la fuente (según DolarApi). */
    @Column(name = "fecha_actualizacion")
    private OffsetDateTime fechaActualizacion;

    /** Última vez que la consultamos y seguía igual. */
    @Column(name = "fecha_consulta", nullable = false)
    private OffsetDateTime fechaConsulta;

    /** true si con esta cotización se ajustaron los precios (o es la primera, la base). */
    @Column(name = "aplicada", nullable = false,
            columnDefinition = "boolean default false not null")
    @Builder.Default
    private boolean aplicada = false;

    /** Porcentaje en que se movieron los precios con esta cotización. null si no se aplicó. */
    @Column(name = "porcentaje_ajuste")
    private Double porcentajeAjuste;

    /** Cuántos productos se ajustaron con esta cotización. */
    @Column(name = "productos_ajustados")
    private Integer productosAjustados;

    /** Cuándo se ajustaron los precios con esta cotización. null si no se aplicó. */
    @Column(name = "fecha_ajuste")
    private OffsetDateTime fechaAjuste;
}
