package com.BlandiArruti.E_commerce.pedido.entity;

import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.entity.Direccion;
import com.BlandiArruti.E_commerce.cupon.entity.Cupon;
import com.BlandiArruti.E_commerce.envio.entity.Envio;
import com.BlandiArruti.E_commerce.enums.ConfirmacionRecepcion;
import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.enums.EstadoReembolso;
import com.BlandiArruti.E_commerce.enums.TipoReembolso;
import com.BlandiArruti.E_commerce.factura.entity.Factura;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cliente", "items", "envio", "factura", "direccionEnvio", "cupon"})

@Entity
@Table(name = "pedidos")
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
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
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_direccion_envio")
    private Direccion direccionEnvio;

    // Costo de envío elegido en el checkout. Se persiste para cobrarlo bien
    // (MercadoPago y la factura) aunque la cotización cambie después.
    // El DEFAULT permite que ddl-auto=update agregue la columna sobre pedidos existentes.
    @Column(name = "costo_envio", nullable = false,
            columnDefinition = "double precision default 0 not null")
    @Builder.Default
    private Double costoEnvio = 0.0;

    // Cupón cargado en el checkout y lo que descontó sobre el subtotal de productos.
    // El descuento queda congelado: si después editan el cupón, el pedido no cambia.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cupon")
    private Cupon cupon;

    // El DEFAULT permite que ddl-auto=update agregue la columna sobre pedidos existentes.
    @Column(name = "descuento_cupon", nullable = false,
            columnDefinition = "double precision default 0 not null")
    @Builder.Default
    private Double descuentoCupon = 0.0;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemPedido> items = new ArrayList<>();

    // El DEFAULT permite que ddl-auto=update agregue la columna sobre pedidos
    // que ya estaban cargados (les queda la fecha de la migración).
    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false,
            columnDefinition = "timestamp default now() not null")
    private LocalDateTime fechaCreacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pedido", nullable = false)
    private EstadoPedido estadoPedido;

    // RF03: el cliente confirma si le llegó el pedido entregado. Null = todavía no respondió.
    // Columnas opcionales: ddl-auto=update las agrega sin problema sobre pedidos existentes.
    @Enumerated(EnumType.STRING)
    @Column(name = "confirmacion_recepcion", length = 20)
    private ConfirmacionRecepcion confirmacionRecepcion;

    @Column(name = "fecha_confirmacion_recepcion")
    private LocalDateTime fechaConfirmacionRecepcion;

    /** Lo que contó el cliente cuando no le llegó, para que lo revise la tienda. */
    @Column(name = "comentario_recepcion", length = 500)
    private String comentarioRecepcion;

    // Cuándo pasó a ENTREGADO. Lo usa el plazo para pedir el reembolso (RF05).
    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    // RF05: reembolso de un pedido entregado. Null = el cliente no lo pidió.
    // Columnas opcionales: ddl-auto=update las agrega sin problema sobre pedidos existentes.
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_reembolso", length = 20)
    private EstadoReembolso estadoReembolso;

    /** Por qué lo pide el cliente. */
    @Column(name = "motivo_reembolso", length = 500)
    private String motivoReembolso;

    @Column(name = "fecha_solicitud_reembolso")
    private LocalDateTime fechaSolicitudReembolso;

    /** Lo que contestó el admin (obligatorio si lo rechaza). */
    @Column(name = "respuesta_reembolso", length = 500)
    private String respuestaReembolso;

    @Column(name = "fecha_resolucion_reembolso")
    private LocalDateTime fechaResolucionReembolso;

    /** Si se aprobó: si se devolvió en Mercado Pago (AUTOMATICO) o lo devuelve la tienda (MANUAL). */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_reembolso", length = 20)
    private TipoReembolso tipoReembolso;

    @Column(name = "monto_reembolsado")
    private Double montoReembolsado;

    /** Días desde la entrega en que el cliente puede pedir el reembolso (derecho de arrepentimiento). */
    public static final int PLAZO_REEMBOLSO_DIAS = 10;

    /** Pasa el pedido a ENTREGADO y guarda cuándo, para el plazo del reembolso. */
    public void marcarEntregado() {
        this.estadoPedido = EstadoPedido.ENTREGADO;
        this.fechaEntrega = LocalDateTime.now();
    }

    /**
     * Hasta cuándo se puede pedir el reembolso. Los pedidos entregados antes del RF05 no tienen
     * fechaEntrega: se usa la llegada prevista del envío. Null si no hay ninguna de las dos.
     */
    public LocalDateTime getFechaLimiteReembolso() {
        LocalDateTime entrega = fechaEntrega;
        if (entrega == null && envio != null && envio.getFechaLlegada() != null) {
            entrega = envio.getFechaLlegada().atTime(LocalTime.MAX);
        }
        return entrega == null ? null : entrega.plusDays(PLAZO_REEMBOLSO_DIAS);
    }

    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Envio envio;

    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Factura factura;
}
