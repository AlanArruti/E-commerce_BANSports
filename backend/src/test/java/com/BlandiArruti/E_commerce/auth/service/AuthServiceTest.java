package com.BlandiArruti.E_commerce.auth.service;

import com.BlandiArruti.E_commerce.administrador.entity.Administrador;
import com.BlandiArruti.E_commerce.auth.dto.request.LoginRequest;
import com.BlandiArruti.E_commerce.auth.dto.response.TokenResponse;
import com.BlandiArruti.E_commerce.auth.util.JwtUtil;
import com.BlandiArruti.E_commerce.cliente.dto.request.ClienteRequest;
import com.BlandiArruti.E_commerce.cliente.dto.response.ClienteResponse;
import com.BlandiArruti.E_commerce.cliente.service.IClienteService;
import com.BlandiArruti.E_commerce.exception.CredencialesInvalidasException;
import com.BlandiArruti.E_commerce.exception.DemasiadosIntentosException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock UsuarioDetailsService usuarioDetailsService;
    @Mock JwtUtil jwtUtil;
    @Mock IClienteService clienteService;
    @Mock LoginAttemptService loginAttemptService;

    @InjectMocks
    AuthService authService;

    private static final String EMAIL = "user@test.com";

    private UsuarioDetails usuarioDetalles() {
        return UsuarioDetails.fromAdmin(
                Administrador.builder().id(1L).username(EMAIL).contrasenia("hash").build());
    }

    // ─── login() ─────────────────────────────────────────────────────────────

    @Test
    void login_credencialesValidas_devuelveTokenConDatosCorrectos() {
        UsuarioDetails details = usuarioDetalles();
        when(loginAttemptService.estaBloqueado(EMAIL)).thenReturn(false);
        when(usuarioDetailsService.loadUserByUsername(EMAIL)).thenReturn(details);
        when(jwtUtil.generarToken(details)).thenReturn("jwt-token-123");

        TokenResponse response = authService.login(new LoginRequest(EMAIL, "pass"));

        assertThat(response.token()).isEqualTo("jwt-token-123");
        assertThat(response.tipo()).isEqualTo("Bearer");
        assertThat(response.id()).isEqualTo(1L);
        verify(loginAttemptService).loginExitoso(EMAIL);
    }

    @Test
    void login_credencialesInvalidas_lanzaExcepcionYRegistraIntentoFallido() {
        when(loginAttemptService.estaBloqueado(EMAIL)).thenReturn(false);
        doThrow(new BadCredentialsException("bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "malapass")))
                .isInstanceOf(CredencialesInvalidasException.class);

        verify(loginAttemptService).loginFallido(EMAIL);
        verify(loginAttemptService, never()).loginExitoso(EMAIL);
    }

    @Test
    void login_cuentaBloqueada_lanzaExcepcionSinLlamarAuthManager() {
        when(loginAttemptService.estaBloqueado(EMAIL)).thenReturn(true);
        when(loginAttemptService.minutosRestantes(EMAIL)).thenReturn(10L);

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "pass")))
                .isInstanceOf(DemasiadosIntentosException.class)
                .hasMessageContaining("10");

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_cuentaDesactivada_lanzaCredencialesInvalidasConMensajeDesactivada() {
        when(loginAttemptService.estaBloqueado(EMAIL)).thenReturn(false);
        doThrow(new DisabledException("account disabled"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "pass")))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessageContaining("desactivada");
    }

    // ─── registrar() ─────────────────────────────────────────────────────────

    @Test
    void registrar_delegaEnClienteServiceYRetornaRespuesta() {
        ClienteRequest request = new ClienteRequest(
                "Ana", "García", "30000000", "ana@test.com", "pass123");
        ClienteResponse expected = new ClienteResponse(
                1L, "uuid-1", "Ana", "García", "30000000", "ana@test.com", List.of());
        when(clienteService.crear(request)).thenReturn(expected);

        ClienteResponse result = authService.registrar(request);

        assertThat(result).isEqualTo(expected);
        verify(clienteService).crear(request);
    }
}
