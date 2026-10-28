package com.BlandiArruti.E_commerce.categoria.service;

import com.BlandiArruti.E_commerce.categoria.dto.request.CategoriaRequest;
import com.BlandiArruti.E_commerce.categoria.dto.response.CategoriaResponse;
import com.BlandiArruti.E_commerce.categoria.dto.response.EliminacionResponse;
import com.BlandiArruti.E_commerce.categoria.entity.Categoria;
import com.BlandiArruti.E_commerce.categoria.mapper.CategoriaMapper;
import com.BlandiArruti.E_commerce.categoria.repository.CategoriaRepository;
import com.BlandiArruti.E_commerce.exception.DuplicadoException;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import com.BlandiArruti.E_commerce.producto.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock CategoriaRepository categoriaRepository;
    @Mock ProductoRepository productoRepository;
    @Mock CategoriaMapper categoriaMapper;

    @InjectMocks
    CategoriaService categoriaService;

    private Categoria categoriaConId(Long id, String nombre) {
        return Categoria.builder().id(id).nombre(nombre).build();
    }

    private CategoriaResponse responseConNombre(String nombre) {
        return new CategoriaResponse(1L, "uuid-" + nombre, nombre);
    }

    // ─── listarTodas() ───────────────────────────────────────────────────────

    @Test
    void listarTodas_sinFiltro_retornaTodas() {
        Categoria c1 = categoriaConId(1L, "Ropa");
        Categoria c2 = categoriaConId(2L, "Electrónica");
        when(categoriaRepository.findAll()).thenReturn(List.of(c1, c2));
        when(categoriaMapper.toResponse(c1)).thenReturn(responseConNombre("Ropa"));
        when(categoriaMapper.toResponse(c2)).thenReturn(responseConNombre("Electrónica"));

        List<CategoriaResponse> result = categoriaService.listarTodas(null);

        assertThat(result).hasSize(2);
        verify(categoriaRepository).findAll();
        verify(categoriaRepository, never()).findByNombreContainingIgnoreCase(any());
    }

    @Test
    void listarTodas_conFiltroNombre_usaBusquedaParcial() {
        Categoria c = categoriaConId(1L, "Electrónica");
        when(categoriaRepository.findByNombreContainingIgnoreCase("electro"))
                .thenReturn(List.of(c));
        when(categoriaMapper.toResponse(c)).thenReturn(responseConNombre("Electrónica"));

        List<CategoriaResponse> result = categoriaService.listarTodas("electro");

        assertThat(result).hasSize(1);
        verify(categoriaRepository).findByNombreContainingIgnoreCase("electro");
        verify(categoriaRepository, never()).findAll();
    }

    // ─── buscarPorId() ───────────────────────────────────────────────────────

    @Test
    void buscarPorId_existente_retornaResponse() {
        Categoria c = categoriaConId(5L, "Ropa");
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(c));
        when(categoriaMapper.toResponse(c)).thenReturn(responseConNombre("Ropa"));

        CategoriaResponse result = categoriaService.buscarPorId(5L);

        assertThat(result.nombre()).isEqualTo("Ropa");
    }

    @Test
    void buscarPorId_inexistente_lanzaEntidadNoEncontrada() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaService.buscarPorId(99L))
                .isInstanceOf(EntidadNoEncontradaException.class);
    }

    // ─── crear() ─────────────────────────────────────────────────────────────

    @Test
    void crear_nombreNuevo_guardaYRetornaResponse() {
        CategoriaRequest request = new CategoriaRequest("Hogar");
        Categoria guardada = categoriaConId(3L, "Hogar");
        when(categoriaRepository.findByNombre("Hogar")).thenReturn(Optional.empty());
        when(categoriaMapper.toEntity(request)).thenReturn(categoriaConId(null, "Hogar"));
        when(categoriaRepository.save(any())).thenReturn(guardada);
        when(categoriaMapper.toResponse(guardada)).thenReturn(responseConNombre("Hogar"));

        CategoriaResponse result = categoriaService.crear(request);

        assertThat(result.nombre()).isEqualTo("Hogar");
        verify(categoriaRepository).save(any());
    }

    @Test
    void crear_nombreDuplicado_lanzaDuplicadoException() {
        CategoriaRequest request = new CategoriaRequest("Ropa");
        when(categoriaRepository.findByNombre("Ropa"))
                .thenReturn(Optional.of(categoriaConId(1L, "Ropa")));

        assertThatThrownBy(() -> categoriaService.crear(request))
                .isInstanceOf(DuplicadoException.class);

        verify(categoriaRepository, never()).save(any());
    }

    // ─── actualizar() ────────────────────────────────────────────────────────

    @Test
    void actualizar_nombreDistintoYLibre_actualizaCategoria() {
        Categoria existente = categoriaConId(1L, "Ropa");
        CategoriaRequest request = new CategoriaRequest("Moda");
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.findByNombre("Moda")).thenReturn(Optional.empty());
        when(categoriaRepository.save(existente)).thenReturn(existente);
        when(categoriaMapper.toResponse(existente)).thenReturn(responseConNombre("Moda"));

        CategoriaResponse result = categoriaService.actualizar(1L, request);

        assertThat(existente.getNombre()).isEqualTo("Moda");
        assertThat(result.nombre()).isEqualTo("Moda");
    }

    @Test
    void actualizar_nombreDuplicado_lanzaDuplicadoException() {
        Categoria existente = categoriaConId(1L, "Ropa");
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.findByNombre("Electrónica"))
                .thenReturn(Optional.of(categoriaConId(2L, "Electrónica")));

        assertThatThrownBy(() -> categoriaService.actualizar(1L, new CategoriaRequest("Electrónica")))
                .isInstanceOf(DuplicadoException.class);
    }

    // ─── eliminar() ──────────────────────────────────────────────────────────

    @Test
    void eliminar_sinConfirmar_conProductos_devuelveAvisoConListaDeAfectados() {
        Categoria c = categoriaConId(1L, "Ropa");
        Producto p = Producto.builder().id(10L).nombre("Remera").build();
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(c));
        when(productoRepository.findByCategoriaId(1L)).thenReturn(List.of(p));

        EliminacionResponse result = categoriaService.eliminar(1L, false);

        assertThat(result.aviso()).hasSize(1);
        assertThat(result.aviso().get(0)).contains("Remera");
        verify(categoriaRepository, never()).delete(any());
    }

    @Test
    void eliminar_sinConfirmar_sinProductos_devuelveMensajeSinAviso() {
        Categoria c = categoriaConId(1L, "Vacía");
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(c));
        when(productoRepository.findByCategoriaId(1L)).thenReturn(List.of());

        EliminacionResponse result = categoriaService.eliminar(1L, false);

        assertThat(result.aviso()).isEmpty();
        verify(categoriaRepository, never()).delete(any());
    }

    @Test
    void eliminar_conConfirmar_desvinculaProductosYElimina() {
        Categoria c = categoriaConId(1L, "Ropa");
        Producto p1 = Producto.builder().id(10L).nombre("Remera").build();
        Producto p2 = Producto.builder().id(11L).nombre("Pantalón").build();
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(c));
        when(productoRepository.findByCategoriaId(1L)).thenReturn(List.of(p1, p2));

        EliminacionResponse result = categoriaService.eliminar(1L, true);

        assertThat(p1.getCategoria()).isNull();
        assertThat(p2.getCategoria()).isNull();
        verify(productoRepository, times(2)).save(any(Producto.class));
        verify(categoriaRepository).delete(c);
        assertThat(result.aviso()).hasSize(2);
    }

    @Test
    void eliminar_conConfirmar_sinProductos_eliminaDirectamente() {
        Categoria c = categoriaConId(1L, "Vacía");
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(c));
        when(productoRepository.findByCategoriaId(1L)).thenReturn(List.of());

        EliminacionResponse result = categoriaService.eliminar(1L, true);

        verify(categoriaRepository).delete(c);
        assertThat(result.aviso()).isEmpty();
        assertThat(result.mensaje()).contains("eliminada correctamente");
    }
}
