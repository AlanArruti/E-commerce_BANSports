package com.BlandiArruti.E_commerce.administrador.service;

import com.BlandiArruti.E_commerce.administrador.dto.request.AdministradorRequest;
import com.BlandiArruti.E_commerce.administrador.dto.response.AdministradorResponse;
import com.BlandiArruti.E_commerce.administrador.entity.Administrador;
import com.BlandiArruti.E_commerce.administrador.mapper.AdministradorMapper;
import com.BlandiArruti.E_commerce.administrador.repository.AdministradorRepository;
import com.BlandiArruti.E_commerce.exception.DuplicadoException;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdministradorServiceTest {

    @Mock AdministradorRepository administradorRepository;
    @Mock AdministradorMapper administradorMapper;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks
    AdministradorService administradorService;

    // ─── crear ───────────────────────────────────────────────────────────────

    @Test
    void crear_usernameNuevo_guardaConContraseniaEncriptada() {
        var request = new AdministradorRequest("nuevoAdmin", "password123");
        Administrador admin = Administrador.builder().id(1L).username("nuevoAdmin").build();
        AdministradorResponse response = new AdministradorResponse(1L, "nuevoAdmin");

        when(administradorRepository.findByUsername("nuevoAdmin")).thenReturn(Optional.empty());
        when(administradorMapper.toEntity(request)).thenReturn(admin);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPwd");
        when(administradorRepository.save(admin)).thenReturn(admin);
        when(administradorMapper.toResponse(admin)).thenReturn(response);

        AdministradorResponse result = administradorService.crear(request);

        assertThat(admin.getContrasenia()).isEqualTo("hashedPwd");
        assertThat(result).isEqualTo(response);
        verify(administradorRepository).save(admin);
    }

    @Test
    void crear_usernameDuplicado_lanzaDuplicadoException() {
        var request = new AdministradorRequest("adminExistente", "pass");
        Administrador existente = Administrador.builder().id(5L).username("adminExistente").build();

        when(administradorRepository.findByUsername("adminExistente")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> administradorService.crear(request))
                .isInstanceOf(DuplicadoException.class);

        verify(administradorRepository, never()).save(any());
    }

    // ─── actualizar ──────────────────────────────────────────────────────────

    @Test
    void actualizar_usernameNuevo_actualizaYEncripta() {
        Administrador admin = Administrador.builder().id(1L).username("viejoAdmin").build();
        var request = new AdministradorRequest("nuevoAdmin", "nuevaPass");
        AdministradorResponse response = new AdministradorResponse(1L, "nuevoAdmin");

        when(administradorRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(administradorRepository.findByUsername("nuevoAdmin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("nuevaPass")).thenReturn("hashedNueva");
        when(administradorRepository.save(admin)).thenReturn(admin);
        when(administradorMapper.toResponse(admin)).thenReturn(response);

        AdministradorResponse result = administradorService.actualizar(1L, request);

        assertThat(admin.getUsername()).isEqualTo("nuevoAdmin");
        assertThat(admin.getContrasenia()).isEqualTo("hashedNueva");
        assertThat(result).isEqualTo(response);
    }

    @Test
    void actualizar_usernamePropio_permiteCambiarContrasenia() {
        Administrador admin = Administrador.builder().id(1L).username("mismoAdmin").build();
        var request = new AdministradorRequest("mismoAdmin", "nuevaPass");
        AdministradorResponse response = new AdministradorResponse(1L, "mismoAdmin");

        when(administradorRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(administradorRepository.findByUsername("mismoAdmin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode("nuevaPass")).thenReturn("hashed");
        when(administradorRepository.save(admin)).thenReturn(admin);
        when(administradorMapper.toResponse(admin)).thenReturn(response);

        // No debe lanzar DuplicadoException porque el username encontrado es el mismo admin (misma id)
        AdministradorResponse result = administradorService.actualizar(1L, request);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void actualizar_usernameOcupadoPorOtro_lanzaDuplicadoException() {
        Administrador admin = Administrador.builder().id(1L).username("admin1").build();
        Administrador otro = Administrador.builder().id(2L).username("admin2").build();
        var request = new AdministradorRequest("admin2", "pass");

        when(administradorRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(administradorRepository.findByUsername("admin2")).thenReturn(Optional.of(otro));

        assertThatThrownBy(() -> administradorService.actualizar(1L, request))
                .isInstanceOf(DuplicadoException.class);

        verify(administradorRepository, never()).save(any());
    }

    @Test
    void actualizar_adminInexistente_lanzaNotFound() {
        when(administradorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                administradorService.actualizar(99L, new AdministradorRequest("x", "y")))
                .isInstanceOf(EntidadNoEncontradaException.class);
    }

    // ─── eliminar ────────────────────────────────────────────────────────────

    @Test
    void eliminar_adminExistente_eliminaFisicamente() {
        Administrador admin = Administrador.builder().id(1L).build();

        when(administradorRepository.findById(1L)).thenReturn(Optional.of(admin));

        administradorService.eliminar(1L);

        verify(administradorRepository).delete(admin);
    }

    @Test
    void eliminar_adminInexistente_lanzaNotFound() {
        when(administradorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> administradorService.eliminar(99L))
                .isInstanceOf(EntidadNoEncontradaException.class);
    }

    // ─── buscarPorId ─────────────────────────────────────────────────────────

    @Test
    void buscarPorId_existente_retornaResponse() {
        Administrador admin = Administrador.builder().id(1L).username("admin").build();
        AdministradorResponse response = new AdministradorResponse(1L, "admin");

        when(administradorRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(administradorMapper.toResponse(admin)).thenReturn(response);

        assertThat(administradorService.buscarPorId(1L)).isEqualTo(response);
    }

    @Test
    void buscarPorId_inexistente_lanzaNotFound() {
        when(administradorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> administradorService.buscarPorId(99L))
                .isInstanceOf(EntidadNoEncontradaException.class);
    }
}
