package com.BlandiArruti.E_commerce.auth.entity;

import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Enlace de un solo uso para restablecer la contraseña.
 * Se guarda solo el hash SHA-256 del token: si se filtra la base, los enlaces no sirven.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "cliente")

@Entity
@Table(name = "tokens_recuperacion")
public class TokenRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "vence", nullable = false)
    private LocalDateTime vence;

    @Column(name = "usado", nullable = false)
    @Builder.Default
    private boolean usado = false;

    public boolean esValido(LocalDateTime ahora) {
        return !usado && ahora.isBefore(vence);
    }
}
