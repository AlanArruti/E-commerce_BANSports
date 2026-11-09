package com.BlandiArruti.E_commerce.auth.service;

import com.BlandiArruti.E_commerce.auth.client.GitHubEmailClient;
import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2ClienteServiceTest {

    @Mock ClienteRepository clienteRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock GitHubEmailClient gitHubEmailClient;

    @InjectMocks
    OAuth2ClienteService service;

    private static final String EMAIL = "ana@gmail.com";
    private static final String TOKEN = "token-del-proveedor";

    private Map<String, Object> google(Object emailVerificado) {
        Map<String, Object> atributos = new HashMap<>();
        atributos.put("email", EMAIL);
        atributos.put("email_verified", emailVerificado);
        atributos.put("given_name", "Ana");
        atributos.put("family_name", "García López");
        return atributos;
    }

    private Cliente clienteExistente(boolean activo) {
        return Cliente.builder().id(5L).nombre("Ana").apellido("García").email(EMAIL)
                .contrasenia("hash").activo(activo).build();
    }

    private Cliente guardado() {
        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(captor.capture());
        return captor.getValue();
    }

    // ─── Google ──────────────────────────────────────────────────────────────

    @Test
    void google_emailNuevo_creaClienteConNombreYApellido() {
        when(clienteRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash-aleatorio");
        when(clienteRepository.save(any())).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(9L);
            return c;
        });

        UsuarioDetails usuario = service.procesar("google", google(true), TOKEN);

        Cliente nuevo = guardado();
        assertThat(nuevo.getNombre()).isEqualTo("Ana");
        assertThat(nuevo.getApellido()).isEqualTo("García López");
        assertThat(nuevo.getEmail()).isEqualTo(EMAIL);
        assertThat(nuevo.getContrasenia()).isEqualTo("hash-aleatorio");
        assertThat(usuario.getId()).isEqualTo(9L);
        assertThat(usuario.getRol().name()).isEqualTo("CLIENTE");
    }

    @Test
    void google_emailDeCuentaExistente_laVinculaSinCrearOtra() {
        when(clienteRepository.findByEmail(EMAIL)).thenReturn(Optional.of(clienteExistente(true)));

        UsuarioDetails usuario = service.procesar("google", google(true), TOKEN);

        assertThat(usuario.getId()).isEqualTo(5L);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void google_emailNoVerificado_rechazaSinTocarLaBase() {
        assertThatThrownBy(() -> service.procesar("google", google(false), TOKEN))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
                .isEqualTo(OAuth2ClienteService.ERROR_EMAIL_NO_VERIFICADO);

        verifyNoInteractions(clienteRepository);
    }

    @Test
    void google_cuentaDesactivada_rechaza() {
        when(clienteRepository.findByEmail(EMAIL)).thenReturn(Optional.of(clienteExistente(false)));

        assertThatThrownBy(() -> service.procesar("google", google(true), TOKEN))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
                .isEqualTo(OAuth2ClienteService.ERROR_CUENTA_DESACTIVADA);
    }

    @Test
    void google_sinApellido_guardaGuionParaCompletarDespues() {
        Map<String, Object> atributos = google(true);
        atributos.remove("family_name");
        when(clienteRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.procesar("google", atributos, TOKEN);

        assertThat(guardado().getApellido()).isEqualTo("-");
    }

    // ─── GitHub ──────────────────────────────────────────────────────────────

    @Test
    void github_emailPublico_separaNombreYApellido() {
        Map<String, Object> atributos = Map.of("email", EMAIL, "name", "Ana García", "login", "anagarcia");
        when(clienteRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.procesar("github", atributos, TOKEN);

        Cliente nuevo = guardado();
        assertThat(nuevo.getNombre()).isEqualTo("Ana");
        assertThat(nuevo.getApellido()).isEqualTo("García");
        verifyNoInteractions(gitHubEmailClient);
    }

    @Test
    void github_emailPrivado_loPideAGitHubConElToken() {
        Map<String, Object> atributos = new HashMap<>();
        atributos.put("email", null);
        atributos.put("login", "anagarcia");
        when(gitHubEmailClient.emailPrincipalVerificado(TOKEN)).thenReturn(Optional.of(EMAIL));
        when(clienteRepository.findByEmail(EMAIL)).thenReturn(Optional.of(clienteExistente(true)));

        UsuarioDetails usuario = service.procesar("github", atributos, TOKEN);

        assertThat(usuario.getUsername()).isEqualTo(EMAIL);
    }

    @Test
    void github_sinEmailVerificado_rechaza() {
        when(gitHubEmailClient.emailPrincipalVerificado(TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.procesar("github", Map.of("login", "anagarcia"), TOKEN))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
                .isEqualTo(OAuth2ClienteService.ERROR_SIN_EMAIL);

        verifyNoInteractions(clienteRepository);
    }

    @Test
    void github_sinNombre_usaElLoginYApellidoGuion() {
        when(clienteRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.procesar("github", Map.of("email", EMAIL, "login", "anagarcia"), TOKEN);

        Cliente nuevo = guardado();
        assertThat(nuevo.getNombre()).isEqualTo("anagarcia");
        assertThat(nuevo.getApellido()).isEqualTo("-");
    }

    @Test
    void proveedorDesconocido_rechaza() {
        assertThatThrownBy(() -> service.procesar("facebook", Map.of("email", EMAIL), TOKEN))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }
}
