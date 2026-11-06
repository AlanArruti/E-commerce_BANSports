package com.BlandiArruti.E_commerce.favorito.service;

import com.BlandiArruti.E_commerce.administrador.entity.Administrador;
import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.repository.ClienteRepository;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.favorito.dto.response.FavoritoResponse;
import com.BlandiArruti.E_commerce.favorito.entity.Favorito;
import com.BlandiArruti.E_commerce.favorito.repository.FavoritoRepository;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import com.BlandiArruti.E_commerce.producto.mapper.ProductoMapper;
import com.BlandiArruti.E_commerce.producto.repository.ProductoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoritoServiceTest {

    @Mock FavoritoRepository favoritoRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock ProductoRepository productoRepository;
    @Mock ProductoMapper productoMapper;

    @InjectMocks
    FavoritoService favoritoService;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private void autenticarComoCliente(Long idCliente) {
        Cliente cliente = Cliente.builder()
                .id(idCliente)
                .email("cliente@test.com")
                .contrasenia("hash")
                .activo(true)
                .build();
        UsuarioDetails details = UsuarioDetails.fromCliente(cliente);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    private void autenticarComoAdmin() {
        UsuarioDetails details = UsuarioDetails.fromAdmin(
                Administrador.builder().id(99L).username("admin").contrasenia("hash").build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    private Producto producto(Long id) {
        return Producto.builder().id(id).nombre("Producto " + id).build();
    }

    private Favorito favorito(Long idCliente, Producto producto) {
        return Favorito.builder().id(10L).cliente(Cliente.builder().id(idCliente).build()).producto(producto).build();
    }

    // ─── listar ──────────────────────────────────────────────────────────────

    @Test
    void listar_devuelveLosFavoritosDelCliente() {
        autenticarComoCliente(1L);
        when(favoritoRepository.findByClienteIdAndProductoActivoTrueOrderByFechaAgregadoDesc(1L))
                .thenReturn(List.of(favorito(1L, producto(5L)), favorito(1L, producto(6L))));

        List<FavoritoResponse> resultado = favoritoService.listar(1L);

        assertThat(resultado).hasSize(2);
        verify(productoMapper, times(2)).toResponse(any(Producto.class));
    }

    @Test
    void listar_deOtroCliente_lanzaAccessDenied() {
        autenticarComoCliente(1L);

        assertThatThrownBy(() -> favoritoService.listar(2L))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(favoritoRepository);
    }

    @Test
    void listar_comoAdmin_puedeVerCualquierCliente() {
        autenticarComoAdmin();
        when(favoritoRepository.findByClienteIdAndProductoActivoTrueOrderByFechaAgregadoDesc(2L))
                .thenReturn(List.of());

        assertThat(favoritoService.listar(2L)).isEmpty();
    }

    // ─── agregar ─────────────────────────────────────────────────────────────

    @Test
    void agregar_productoNuevo_loGuarda() {
        autenticarComoCliente(1L);
        Producto producto = producto(5L);
        when(productoRepository.findByIdAndActivoTrue(5L)).thenReturn(Optional.of(producto));
        when(favoritoRepository.findByClienteIdAndProductoId(1L, 5L)).thenReturn(Optional.empty());
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(Cliente.builder().id(1L).build()));
        when(favoritoRepository.save(any(Favorito.class))).thenAnswer(inv -> inv.getArgument(0));

        favoritoService.agregar(1L, 5L);

        verify(favoritoRepository).save(any(Favorito.class));
    }

    @Test
    void agregar_productoQueYaEstaba_noLoDuplica() {
        autenticarComoCliente(1L);
        Producto producto = producto(5L);
        when(productoRepository.findByIdAndActivoTrue(5L)).thenReturn(Optional.of(producto));
        when(favoritoRepository.findByClienteIdAndProductoId(1L, 5L))
                .thenReturn(Optional.of(favorito(1L, producto)));

        favoritoService.agregar(1L, 5L);

        verify(favoritoRepository, never()).save(any());
    }

    @Test
    void agregar_productoInexistenteODadoDeBaja_lanzaNoEncontrado() {
        autenticarComoCliente(1L);
        when(productoRepository.findByIdAndActivoTrue(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoritoService.agregar(1L, 5L))
                .isInstanceOf(EntidadNoEncontradaException.class);
        verify(favoritoRepository, never()).save(any());
    }

    @Test
    void agregar_aOtroCliente_lanzaAccessDenied() {
        autenticarComoCliente(1L);

        assertThatThrownBy(() -> favoritoService.agregar(2L, 5L))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(productoRepository, favoritoRepository);
    }

    // ─── quitar ──────────────────────────────────────────────────────────────

    @Test
    void quitar_favoritoExistente_loBorra() {
        autenticarComoCliente(1L);
        Favorito favorito = favorito(1L, producto(5L));
        when(favoritoRepository.findByClienteIdAndProductoId(1L, 5L)).thenReturn(Optional.of(favorito));

        favoritoService.quitar(1L, 5L);

        verify(favoritoRepository).delete(favorito);
    }

    @Test
    void quitar_productoQueNoEstaba_noHaceNada() {
        autenticarComoCliente(1L);
        when(favoritoRepository.findByClienteIdAndProductoId(1L, 5L)).thenReturn(Optional.empty());

        favoritoService.quitar(1L, 5L);

        verify(favoritoRepository, never()).delete(any());
    }
}
