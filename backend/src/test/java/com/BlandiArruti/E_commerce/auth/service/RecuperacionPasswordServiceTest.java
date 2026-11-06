package com.BlandiArruti.E_commerce.auth.service;

import com.BlandiArruti.E_commerce.auth.dto.request.RecuperarPasswordRequest;
import com.BlandiArruti.E_commerce.auth.dto.request.RestablecerPasswordRequest;
import com.BlandiArruti.E_commerce.auth.entity.TokenRecuperacion;
import com.BlandiArruti.E_commerce.auth.repository.TokenRecuperacionRepository;
import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.repository.ClienteRepository;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.notificacion.service.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecuperacionPasswordServiceTest {

    @Mock ClienteRepository clienteRepository;
    @Mock TokenRecuperacionRepository tokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock NotificacionService notificacionService;
    @Mock LoginAttemptService loginAttemptService;

    @InjectMocks
    RecuperacionPasswordService service;

    private static final String EMAIL = "cliente@test.com";
    private static final String TOKEN = "token-del-enlace";

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "frontUrl", "http://localhost:4200/");
        cliente = Cliente.builder().id(7L).nombre("Ana").email(EMAIL).contrasenia("hash-viejo").build();
    }

    private TokenRecuperacion token(LocalDateTime vence, boolean usado) {
        return TokenRecuperacion.builder()
                .cliente(cliente)
                .tokenHash(RecuperacionPasswordService.hash(TOKEN))
                .fechaCreacion(vence.minusMinutes(30))
                .vence(vence)
                .usado(usado)
                .build();
    }

    // ─── solicitar() ─────────────────────────────────────────────────────────

    @Test
    void solicitar_emailExistente_guardaHashYMandaEnlace() {
        when(clienteRepository.findByEmail(EMAIL)).thenReturn(Optional.of(cliente));
        when(tokenRepository.findFirstByClienteOrderByFechaCreacionDesc(cliente)).thenReturn(Optional.empty());

        service.solicitar(new RecuperarPasswordRequest(" " + EMAIL + " "));

        ArgumentCaptor<TokenRecuperacion> guardado = ArgumentCaptor.forClass(TokenRecuperacion.class);
        verify(tokenRepository).invalidarPendientes(cliente);
        verify(tokenRepository).save(guardado.capture());

        ArgumentCaptor<String> enlace = ArgumentCaptor.forClass(String.class);
        verify(notificacionService).notificarRecuperacionPassword(eq(cliente), enlace.capture(), eq(30));

        assertThat(enlace.getValue()).startsWith("http://localhost:4200/restablecer-contrasena?token=");
        String tokenPlano = enlace.getValue().substring(enlace.getValue().indexOf("token=") + 6);
        // En la base queda el hash, nunca el token del enlace.
        assertThat(guardado.getValue().getTokenHash())
                .isEqualTo(RecuperacionPasswordService.hash(tokenPlano))
                .isNotEqualTo(tokenPlano);
        assertThat(guardado.getValue().getVence())
                .isEqualTo(guardado.getValue().getFechaCreacion().plusMinutes(30));
        assertThat(guardado.getValue().isUsado()).isFalse();
    }

    @Test
    void solicitar_emailInexistente_noHaceNadaNiLanzaExcepcion() {
        when(clienteRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        service.solicitar(new RecuperarPasswordRequest(EMAIL));

        verifyNoInteractions(tokenRepository, notificacionService);
    }

    @Test
    void solicitar_clienteDadoDeBaja_noMandaMail() {
        cliente.setActivo(false);
        when(clienteRepository.findByEmail(EMAIL)).thenReturn(Optional.of(cliente));

        service.solicitar(new RecuperarPasswordRequest(EMAIL));

        verifyNoInteractions(tokenRepository, notificacionService);
    }

    @Test
    void solicitar_pedidoRepetidoEnMenosDeUnMinuto_noReenvia() {
        TokenRecuperacion reciente = TokenRecuperacion.builder()
                .cliente(cliente).tokenHash("x")
                .fechaCreacion(LocalDateTime.now().minusSeconds(10))
                .vence(LocalDateTime.now().plusMinutes(29))
                .build();
        when(clienteRepository.findByEmail(EMAIL)).thenReturn(Optional.of(cliente));
        when(tokenRepository.findFirstByClienteOrderByFechaCreacionDesc(cliente)).thenReturn(Optional.of(reciente));

        service.solicitar(new RecuperarPasswordRequest(EMAIL));

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(notificacionService);
    }

    // ─── restablecer() ───────────────────────────────────────────────────────

    @Test
    void restablecer_tokenValido_cambiaContraseniaYMarcaUsado() {
        TokenRecuperacion vigente = token(LocalDateTime.now().plusMinutes(10), false);
        when(tokenRepository.findByTokenHash(RecuperacionPasswordService.hash(TOKEN))).thenReturn(Optional.of(vigente));
        when(passwordEncoder.encode("nueva123")).thenReturn("hash-nuevo");

        service.restablecer(new RestablecerPasswordRequest(TOKEN, "nueva123"));

        assertThat(cliente.getContrasenia()).isEqualTo("hash-nuevo");
        assertThat(vigente.isUsado()).isTrue();
        verify(loginAttemptService).loginExitoso(EMAIL);
    }

    @Test
    void restablecer_tokenVencido_lanzaExcepcionYNoCambiaNada() {
        TokenRecuperacion vencido = token(LocalDateTime.now().minusMinutes(1), false);
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(vencido));

        assertThatThrownBy(() -> service.restablecer(new RestablecerPasswordRequest(TOKEN, "nueva123")))
                .isInstanceOf(EcommerceException.class)
                .hasMessage(RecuperacionPasswordService.TOKEN_INVALIDO);

        assertThat(cliente.getContrasenia()).isEqualTo("hash-viejo");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void restablecer_tokenYaUsado_lanzaExcepcion() {
        TokenRecuperacion usado = token(LocalDateTime.now().plusMinutes(10), true);
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(usado));

        assertThatThrownBy(() -> service.restablecer(new RestablecerPasswordRequest(TOKEN, "nueva123")))
                .isInstanceOf(EcommerceException.class)
                .hasMessage(RecuperacionPasswordService.TOKEN_INVALIDO);

        assertThat(cliente.getContrasenia()).isEqualTo("hash-viejo");
    }

    @Test
    void restablecer_tokenInexistente_lanzaExcepcion() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.restablecer(new RestablecerPasswordRequest("cualquiera", "nueva123")))
                .isInstanceOf(EcommerceException.class);

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void restablecer_clienteDadoDeBaja_lanzaExcepcion() {
        cliente.setActivo(false);
        TokenRecuperacion vigente = token(LocalDateTime.now().plusMinutes(10), false);
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(vigente));

        assertThatThrownBy(() -> service.restablecer(new RestablecerPasswordRequest(TOKEN, "nueva123")))
                .isInstanceOf(EcommerceException.class);

        assertThat(vigente.isUsado()).isFalse();
    }
}
