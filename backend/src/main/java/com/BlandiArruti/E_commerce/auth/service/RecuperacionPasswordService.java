package com.BlandiArruti.E_commerce.auth.service;

import com.BlandiArruti.E_commerce.auth.dto.request.RecuperarPasswordRequest;
import com.BlandiArruti.E_commerce.auth.dto.request.RestablecerPasswordRequest;
import com.BlandiArruti.E_commerce.auth.entity.TokenRecuperacion;
import com.BlandiArruti.E_commerce.auth.repository.TokenRecuperacionRepository;
import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.repository.ClienteRepository;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.notificacion.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Recuperar la contraseña por mail (SCRUM-143).
 *
 * 1. {@link #solicitar}: genera un token aleatorio, guarda su hash y manda el enlace por mail.
 *    Responde igual exista o no el email, para no revelar qué cuentas hay.
 * 2. {@link #restablecer}: con el token del enlace se elige la contraseña nueva.
 *    El token es de un solo uso y vence a los 30 minutos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecuperacionPasswordService {

    static final int MINUTOS_VALIDEZ = 30;
    /** Evita que se use el formulario para llenarle la casilla de mails a alguien. */
    static final int SEGUNDOS_ENTRE_PEDIDOS = 60;
    static final String TOKEN_INVALIDO = "El enlace no es válido o ya venció. Pedí uno nuevo.";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClienteRepository clienteRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificacionService notificacionService;
    private final LoginAttemptService loginAttemptService;

    @Value("${app.front-url:http://localhost:4200}")
    private String frontUrl;

    @Transactional
    public void solicitar(RecuperarPasswordRequest request) {
        Optional<Cliente> encontrado = clienteRepository.findByEmail(request.email().trim())
                .filter(Cliente::isActivo);
        if (encontrado.isEmpty()) {
            log.info("Recuperación de contraseña pedida para un email sin cuenta activa.");
            return;
        }
        Cliente cliente = encontrado.get();
        LocalDateTime ahora = LocalDateTime.now();

        boolean pidioHacePoco = tokenRepository.findFirstByClienteOrderByFechaCreacionDesc(cliente)
                .filter(t -> t.esValido(ahora))
                .filter(t -> t.getFechaCreacion().isAfter(ahora.minusSeconds(SEGUNDOS_ENTRE_PEDIDOS)))
                .isPresent();
        if (pidioHacePoco) {
            log.info("Recuperación de contraseña repetida para el cliente {}: no se reenvía.", cliente.getId());
            return;
        }

        tokenRepository.invalidarPendientes(cliente);

        String token = generarToken();
        tokenRepository.save(TokenRecuperacion.builder()
                .cliente(cliente)
                .tokenHash(hash(token))
                .fechaCreacion(ahora)
                .vence(ahora.plusMinutes(MINUTOS_VALIDEZ))
                .build());

        String enlace = quitarBarraFinal(frontUrl) + "/restablecer-contrasena?token=" + token;
        notificacionService.notificarRecuperacionPassword(cliente, enlace, MINUTOS_VALIDEZ);
    }

    @Transactional
    public void restablecer(RestablecerPasswordRequest request) {
        TokenRecuperacion token = tokenRepository.findByTokenHash(hash(request.token().trim()))
                .filter(t -> t.esValido(LocalDateTime.now()))
                .orElseThrow(() -> new EcommerceException(TOKEN_INVALIDO));

        Cliente cliente = token.getCliente();
        if (!cliente.isActivo()) {
            throw new EcommerceException(TOKEN_INVALIDO);
        }

        cliente.setContrasenia(passwordEncoder.encode(request.contraseniaNueva()));
        token.setUsado(true);
        // Si se bloqueó por intentos fallidos, con la contraseña nueva puede entrar ya.
        loginAttemptService.loginExitoso(cliente.getEmail());
    }

    private static String generarToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private static String quitarBarraFinal(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
