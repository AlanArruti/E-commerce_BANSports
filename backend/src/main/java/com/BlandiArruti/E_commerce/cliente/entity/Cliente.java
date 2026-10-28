package com.BlandiArruti.E_commerce.cliente.entity;

import com.BlandiArruti.E_commerce.carrito.entity.Carrito;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
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
@ToString(exclude = {"direcciones", "pedidos", "carrito"})

@Entity
@Table (name = "clientes")
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "uuid", nullable = false, unique = true, updatable = false, length = 36)
    private String uuid;

    @PrePersist
    private void generarUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
    }
    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;
    @Column(name = "apellido", nullable = false, length = 50)
    private String apellido;
    @Column(name = "dni", nullable = true, unique = true, length = 20)
    private String dni;
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;
    @Column(name = "contrasenia", nullable = false, length = 255)
    private String contrasenia;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Direccion> direcciones = new ArrayList<>();
    @OneToMany(mappedBy = "cliente", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Pedido> pedidos = new ArrayList<>();

    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Carrito carrito;
}

