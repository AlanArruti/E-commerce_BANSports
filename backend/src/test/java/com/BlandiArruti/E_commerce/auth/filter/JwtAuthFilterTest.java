package com.BlandiArruti.E_commerce.auth.filter;

import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.auth.service.UsuarioDetailsService;
import com.BlandiArruti.E_commerce.auth.util.JwtUtil;
import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthFilterTest {

    private final UsuarioDetailsService usuarios = mock(UsuarioDetailsService.class);
    private JwtUtil jwtUtil;
    private JwtAuthFilter filtro;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "clave-de-prueba-para-los-tests-de-al-menos-32-caracteres");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 60_000L);
        filtro = new JwtAuthFilter(jwtUtil, usuarios);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private void filtrarCon(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/pedido");
        request.addHeader("Authorization", "Bearer " + token);
        filtro.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    private static Cliente cliente(boolean activo) {
        return Cliente.builder().id(5L).email("ana@mail.com").contrasenia("x").activo(activo).build();
    }

    @Test
    void cuentaActiva_conTokenValido_quedaAutenticada() throws Exception {
        String token = jwtUtil.generarToken(UsuarioDetails.fromCliente(cliente(true)));
        when(usuarios.loadUserByUsername("ana@mail.com")).thenReturn(UsuarioDetails.fromCliente(cliente(true)));

        filtrarCon(token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void cuentaDadaDeBaja_conTokenTodaviaVigente_noQuedaAutenticada() throws Exception {
        String token = jwtUtil.generarToken(UsuarioDetails.fromCliente(cliente(true)));
        // Después de emitir el token, la cuenta se dio de baja.
        when(usuarios.loadUserByUsername("ana@mail.com")).thenReturn(UsuarioDetails.fromCliente(cliente(false)));

        filtrarCon(token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void cuentaQueYaNoExiste_noRompeLaRequest_niQuedaAutenticada() {
        String token = jwtUtil.generarToken(UsuarioDetails.fromCliente(cliente(true)));
        // El cliente cambió el email (o se borró la cuenta) después de emitir el token.
        when(usuarios.loadUserByUsername("ana@mail.com")).thenThrow(new UsernameNotFoundException("no existe"));

        assertThatCode(() -> filtrarCon(token)).doesNotThrowAnyException();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
