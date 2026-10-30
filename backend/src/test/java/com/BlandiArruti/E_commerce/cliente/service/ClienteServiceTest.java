package com.BlandiArruti.E_commerce.cliente.service;

import com.BlandiArruti.E_commerce.administrador.entity.Administrador;
import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.cliente.dto.request.CambioPasswordRequest;
import com.BlandiArruti.E_commerce.cliente.dto.request.ClienteRequest;
import com.BlandiArruti.E_commerce.cliente.dto.request.ClienteUpdateRequest;
import com.BlandiArruti.E_commerce.cliente.dto.response.ClienteResponse;
import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.mapper.ClienteMapper;
import com.BlandiArruti.E_commerce.cliente.mapper.DireccionMapper;
import com.BlandiArruti.E_commerce.cliente.repository.ClienteRepository;
import com.BlandiArruti.E_commerce.cliente.repository.DireccionRepository;
import com.BlandiArruti.E_commerce.exception.CredencialesInvalidasException;
import com.BlandiArruti.E_commerce.exception.DuplicadoException;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.geo.service.LocalidadService;
import com.BlandiArruti.E_commerce.notificacion.service.NotificacionService;
import com.BlandiArruti.E_commerce.pedido.mapper.PedidoMapper;
import com.BlandiArruti.E_commerce.pedido.repository.PedidoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock ClienteRepository clienteRepository;
    @Mock DireccionRepository direccionRepository;
    @Mock LocalidadService localidadService;
    @Mock PedidoRepository pedidoRepository;
    @Mock ClienteMapper clienteMapper;
    @Mock DireccionMapper direccionMapper;
    @Mock PedidoMapper pedidoMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock NotificacionService notificacionService;

    @InjectMocks
    ClienteService clienteService;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    // ─── crear ───────────────────────────────────────────────────────────────

    @Test
    void crear_clienteNuevo_guardaYEnviaNotificacion() {
        // ClienteRequest(nombre, apellido, dni, email, contrasenia)
        var request = new ClienteRequest("Juan", "Pérez", "12345678", "juan@mail.com", "pass123");
        Cliente cliente = Cliente.builder().id(1L).nombre("Juan").email("juan@mail.com").build();
        ClienteResponse response = mock(ClienteResponse.class);

        when(clienteRepository.existsByEmail("juan@mail.com")).thenReturn(false);
        when(clienteRepository.existsByDni("12345678")).thenReturn(false);
        when(clienteMapper.toEntity(request)).thenReturn(cliente);
        when(passwordEncoder.encode("pass123")).thenReturn("hashed");
        when(clienteRepository.save(cliente)).thenReturn(cliente);
        when(clienteMapper.toResponse(cliente)).thenReturn(response);

        ClienteResponse result = clienteService.crear(request);

        assertThat(cliente.getContrasenia()).isEqualTo("hashed");
        assertThat(result).isEqualTo(response);
        verify(notificacionService).notificarRegistroExitoso(cliente);
    }

    @Test
    void crear_emailDuplicado_lanzaDuplicadoException() {
        var request = new ClienteRequest("Juan", "Pérez", "12345678", "duplicado@mail.com", "pass");

        when(clienteRepository.existsByEmail("duplicado@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> clienteService.crear(request))
                .isInstanceOf(DuplicadoException.class);

        verify(clienteRepository, never()).save(any());
        verify(notificacionService, never()).notificarRegistroExitoso(any());
    }

    @Test
    void crear_dniDuplicado_lanzaDuplicadoException() {
        var request = new ClienteRequest("Juan", "Pérez", "99999999", "unico@mail.com", "pass");

        when(clienteRepository.existsByEmail("unico@mail.com")).thenReturn(false);
        when(clienteRepository.existsByDni("99999999")).thenReturn(true);

        assertThatThrownBy(() -> clienteService.crear(request))
                .isInstanceOf(DuplicadoException.class);

        verify(notificacionService, never()).notificarRegistroExitoso(any());
    }

    // ─── buscarPorId — control de propietario ────────────────────────────────

    @Test
    void buscarPorId_clienteAccedeASuPropioPerfil_retornaResponse() {
        Cliente cliente = Cliente.builder().id(1L).activo(true).build();
        ClienteResponse response = mock(ClienteResponse.class);

        autenticarComoCliente(1L);
        when(clienteRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(cliente));
        when(clienteMapper.toResponse(cliente)).thenReturn(response);

        assertThat(clienteService.buscarPorId(1L)).isEqualTo(response);
    }

    @Test
    void buscarPorId_clienteAccedeAOtroPerfil_lanzaAccessDenied() {
        autenticarComoCliente(1L);

        assertThatThrownBy(() -> clienteService.buscarPorId(2L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void buscarPorId_adminAccedeACualquierPerfil_retornaResponse() {
        Cliente cliente = Cliente.builder().id(5L).activo(true).build();
        ClienteResponse response = mock(ClienteResponse.class);

        autenticarComoAdmin();
        when(clienteRepository.findByIdAndActivoTrue(5L)).thenReturn(Optional.of(cliente));
        when(clienteMapper.toResponse(cliente)).thenReturn(response);

        assertThat(clienteService.buscarPorId(5L)).isEqualTo(response);
    }

    // ─── actualizar ──────────────────────────────────────────────────────────

    @Test
    void actualizar_emailNuevo_actualizaDatos() {
        Cliente cliente = Cliente.builder().id(1L).email("viejo@mail.com")
                .contrasenia("hash-original").activo(true).build();
        var request = new ClienteUpdateRequest("Juan", "Pérez", "nuevo@mail.com");
        ClienteResponse response = mock(ClienteResponse.class);

        autenticarComoAdmin();
        when(clienteRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.existsByEmail("nuevo@mail.com")).thenReturn(false);
        when(clienteRepository.save(cliente)).thenReturn(cliente);
        when(clienteMapper.toResponse(cliente)).thenReturn(response);

        ClienteResponse result = clienteService.actualizar(1L, request);

        assertThat(cliente.getEmail()).isEqualTo("nuevo@mail.com");
        assertThat(result).isEqualTo(response);
    }

    @Test
    void actualizar_noTocaLaContrasenia() {
        Cliente cliente = Cliente.builder().id(1L).email("viejo@mail.com")
                .contrasenia("hash-original").activo(true).build();
        var request = new ClienteUpdateRequest("Juan", "Pérez", "nuevo@mail.com");

        autenticarComoAdmin();
        when(clienteRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.existsByEmail("nuevo@mail.com")).thenReturn(false);
        when(clienteRepository.save(cliente)).thenReturn(cliente);

        clienteService.actualizar(1L, request);

        assertThat(cliente.getContrasenia()).isEqualTo("hash-original");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void actualizar_emailDuplicadoDeOtroCliente_lanzaDuplicado() {
        Cliente cliente = Cliente.builder().id(1L).email("original@mail.com").activo(true).build();
        var request = new ClienteUpdateRequest("Juan", "Pérez", "otro@mail.com");

        autenticarComoAdmin();
        when(clienteRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.existsByEmail("otro@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> clienteService.actualizar(1L, request))
                .isInstanceOf(DuplicadoException.class);
    }

    // ─── cambiarPassword ─────────────────────────────────────────────────────

    @Test
    void cambiarPassword_conContraseniaActualCorrecta_laReemplaza() {
        Cliente cliente = Cliente.builder().id(1L).contrasenia("hash-viejo").activo(true).build();
        var request = new CambioPasswordRequest("vieja", "nueva123");

        autenticarComoAdmin();
        when(clienteRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(cliente));
        when(passwordEncoder.matches("vieja", "hash-viejo")).thenReturn(true);
        when(passwordEncoder.encode("nueva123")).thenReturn("hash-nuevo");

        clienteService.cambiarPassword(1L, request);

        assertThat(cliente.getContrasenia()).isEqualTo("hash-nuevo");
        verify(clienteRepository).save(cliente);
    }

    @Test
    void cambiarPassword_conContraseniaActualIncorrecta_lanzaCredencialesInvalidas() {
        Cliente cliente = Cliente.builder().id(1L).contrasenia("hash-viejo").activo(true).build();
        var request = new CambioPasswordRequest("equivocada", "nueva123");

        autenticarComoAdmin();
        when(clienteRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(cliente));
        when(passwordEncoder.matches("equivocada", "hash-viejo")).thenReturn(false);

        assertThatThrownBy(() -> clienteService.cambiarPassword(1L, request))
                .isInstanceOf(CredencialesInvalidasException.class);
        verify(clienteRepository, never()).save(any());
    }

    // ─── eliminar ────────────────────────────────────────────────────────────

    @Test
    void eliminar_propioCliente_haceSoftDelete() {
        Cliente cliente = Cliente.builder().id(1L).activo(true).build();

        autenticarComoCliente(1L);
        when(clienteRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        clienteService.eliminar(1L);

        assertThat(cliente.isActivo()).isFalse();
    }

    @Test
    void eliminar_otroCliente_lanzaAccessDenied() {
        autenticarComoCliente(1L);

        assertThatThrownBy(() -> clienteService.eliminar(2L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void eliminar_clienteInexistente_lanzaNotFound() {
        autenticarComoAdmin();
        when(clienteRepository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.eliminar(99L))
                .isInstanceOf(EntidadNoEncontradaException.class);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private void autenticarComoCliente(Long id) {
        Cliente cliente = Cliente.builder().id(id).email("test@mail.com")
                .contrasenia("x").activo(true).build();
        UsuarioDetails details = UsuarioDetails.fromCliente(cliente);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    private void autenticarComoAdmin() {
        UsuarioDetails admin = UsuarioDetails.fromAdmin(
                Administrador.builder().id(99L).username("admin").contrasenia("x").build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));
    }
}
