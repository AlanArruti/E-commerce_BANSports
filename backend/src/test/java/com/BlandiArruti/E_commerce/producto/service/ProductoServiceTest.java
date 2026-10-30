package com.BlandiArruti.E_commerce.producto.service;

import com.BlandiArruti.E_commerce.categoria.entity.Categoria;
import com.BlandiArruti.E_commerce.categoria.repository.CategoriaRepository;
import com.BlandiArruti.E_commerce.enums.OperacionStock;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.exception.StockInsuficienteException;
import com.BlandiArruti.E_commerce.producto.dto.request.StockRequest;
import com.BlandiArruti.E_commerce.producto.dto.request.VarianteRequest;
import com.BlandiArruti.E_commerce.producto.entity.ImagenProducto;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import com.BlandiArruti.E_commerce.producto.entity.Variante;
import com.BlandiArruti.E_commerce.producto.mapper.ProductoMapper;
import com.BlandiArruti.E_commerce.producto.mapper.VarianteMapper;
import com.BlandiArruti.E_commerce.producto.repository.ImagenProductoRepository;
import com.BlandiArruti.E_commerce.producto.repository.ProductoRepository;
import com.BlandiArruti.E_commerce.producto.repository.VarianteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock ProductoRepository productoRepository;
    @Mock CategoriaRepository categoriaRepository;
    @Mock VarianteRepository varianteRepository;
    @Mock ImagenProductoRepository imagenProductoRepository;
    @Mock ProductoMapper productoMapper;
    @Mock VarianteMapper varianteMapper;

    @InjectMocks
    ProductoService productoService;

    // ─── actualizarVariante — ownership ──────────────────────────────────────

    @Test
    void actualizarVariante_varianteDeOtroProducto_lanzaNotFound() {
        Producto producto1 = Producto.builder().id(1L).activo(true).build();
        Producto producto2 = Producto.builder().id(2L).activo(true).build();
        Variante variante = Variante.builder().id(10L).activo(true).producto(producto2).build();

        when(productoRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(producto1));
        when(varianteRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(variante));

        var request = new VarianteRequest(Map.of("talle", "M"), 5);

        assertThatThrownBy(() -> productoService.actualizarVariante(1L, 10L, request))
                .isInstanceOf(EntidadNoEncontradaException.class);
    }

    @Test
    void actualizarVariante_varianteDelProductoCorrecto_actualizaYRetorna() {
        Producto producto = Producto.builder().id(1L).activo(true).build();
        Variante variante = Variante.builder().id(10L).activo(true).producto(producto).stock(3).build();

        when(productoRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(producto));
        when(varianteRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(variante));
        when(varianteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(varianteMapper.toResponse(any())).thenReturn(null);

        productoService.actualizarVariante(1L, 10L, new VarianteRequest(Map.of("color", "rojo"), 8));

        assertThat(variante.getStock()).isEqualTo(8);
        verify(varianteRepository).save(variante);
    }

    // ─── eliminarVariante — ownership ────────────────────────────────────────

    @Test
    void eliminarVariante_varianteDeOtroProducto_lanzaNotFound() {
        Producto producto1 = Producto.builder().id(1L).activo(true).build();
        Producto producto2 = Producto.builder().id(2L).activo(true).build();
        Variante variante = Variante.builder().id(10L).activo(true).producto(producto2).build();

        when(productoRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(producto1));
        when(varianteRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(variante));

        assertThatThrownBy(() -> productoService.eliminarVariante(1L, 10L))
                .isInstanceOf(EntidadNoEncontradaException.class);

        verify(varianteRepository, never()).save(any());
    }

    @Test
    void eliminarVariante_varianteDelProductoCorrecto_haceSoftDelete() {
        Producto producto = Producto.builder().id(1L).activo(true).build();
        Variante variante = Variante.builder().id(10L).activo(true).producto(producto).build();

        when(productoRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(producto));
        when(varianteRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(variante));
        when(varianteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        productoService.eliminarVariante(1L, 10L);

        assertThat(variante.isActivo()).isFalse();
    }

    // ─── ajustarStock — ownership + lógica ───────────────────────────────────

    @Test
    void ajustarStock_varianteDeOtroProducto_lanzaNotFound() {
        Producto producto1 = Producto.builder().id(1L).activo(true).build();
        Producto producto2 = Producto.builder().id(2L).activo(true).build();
        Variante variante = Variante.builder().id(10L).activo(true).producto(producto2).stock(5).build();

        when(productoRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(producto1));
        when(varianteRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(variante));

        assertThatThrownBy(() ->
                productoService.ajustarStock(1L, 10L, new StockRequest(3, OperacionStock.AGREGAR)))
                .isInstanceOf(EntidadNoEncontradaException.class);
    }

    @Test
    void ajustarStock_agregar_incrementaStock() {
        Producto producto = Producto.builder().id(1L).activo(true).build();
        Variante variante = Variante.builder().id(10L).activo(true).producto(producto).stock(5).build();

        when(productoRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(producto));
        when(varianteRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(variante));
        when(varianteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(varianteMapper.toResponse(any())).thenReturn(null);

        productoService.ajustarStock(1L, 10L, new StockRequest(3, OperacionStock.AGREGAR));

        assertThat(variante.getStock()).isEqualTo(8);
    }

    @Test
    void ajustarStock_reducir_decrementaStock() {
        Producto producto = Producto.builder().id(1L).activo(true).build();
        Variante variante = Variante.builder().id(10L).activo(true).producto(producto).stock(10).build();

        when(productoRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(producto));
        when(varianteRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(variante));
        when(varianteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(varianteMapper.toResponse(any())).thenReturn(null);

        productoService.ajustarStock(1L, 10L, new StockRequest(4, OperacionStock.REDUCIR));

        assertThat(variante.getStock()).isEqualTo(6);
    }

    @Test
    void ajustarStock_reducirMasQueStock_lanzaStockInsuficiente() {
        Producto producto = Producto.builder().id(1L).activo(true).build();
        Variante variante = Variante.builder().id(10L).activo(true).producto(producto).stock(2).build();

        when(productoRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(producto));
        when(varianteRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(variante));

        assertThatThrownBy(() ->
                productoService.ajustarStock(1L, 10L, new StockRequest(5, OperacionStock.REDUCIR)))
                .isInstanceOf(StockInsuficienteException.class);
    }

    // ─── eliminar producto — soft delete ─────────────────────────────────────

    @Test
    void eliminar_productoExistente_haceSoftDelete() {
        Producto producto = Producto.builder().id(1L).activo(true).build();

        when(productoRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        productoService.eliminar(1L);

        assertThat(producto.isActivo()).isFalse();
    }

    @Test
    void eliminar_productoInexistente_lanzaNotFound() {
        when(productoRepository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.eliminar(99L))
                .isInstanceOf(EntidadNoEncontradaException.class);
    }

    // ─── listarVariantes — producto inexistente ───────────────────────────────

    @Test
    void listarVariantes_productoInexistente_lanzaNotFound() {
        when(productoRepository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.listarVariantes(99L))
                .isInstanceOf(EntidadNoEncontradaException.class);
    }

    // ─── imágenes de producto ────────────────────────────────────────────────

    @Test
    void subirImagen_archivoValido_guardaYMarcaElProducto() {
        Producto producto = Producto.builder().id(1L).activo(true).build();
        var archivo = new MockMultipartFile(
                "archivo", "foto.png", "image/png", new byte[]{1, 2, 3});

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(imagenProductoRepository.findByProductoId(1L)).thenReturn(Optional.empty());

        var response = productoService.subirImagen(1L, archivo);

        assertThat(producto.isTieneImagen()).isTrue();
        assertThat(response.contentType()).isEqualTo("image/png");
        assertThat(response.tamanioBytes()).isEqualTo(3);
        assertThat(response.url()).isEqualTo("/api/v1/producto/1/imagen");
        verify(imagenProductoRepository).save(any(ImagenProducto.class));
    }

    @Test
    void subirImagen_reemplazaLaExistente() {
        Producto producto = Producto.builder().id(1L).activo(true).tieneImagen(true).build();
        ImagenProducto previa = ImagenProducto.builder()
                .id(7L).producto(producto).nombreArchivo("vieja.png")
                .contentType("image/png").datos(new byte[]{9}).build();
        var archivo = new MockMultipartFile(
                "archivo", "nueva.jpg", "image/jpeg", new byte[]{1, 2});

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(imagenProductoRepository.findByProductoId(1L)).thenReturn(Optional.of(previa));

        productoService.subirImagen(1L, archivo);

        assertThat(previa.getNombreArchivo()).isEqualTo("nueva.jpg");
        assertThat(previa.getContentType()).isEqualTo("image/jpeg");
        assertThat(previa.getDatos()).containsExactly(1, 2);
    }

    @Test
    void subirImagen_formatoNoSoportado_lanzaEcommerceException() {
        Producto producto = Producto.builder().id(1L).activo(true).build();
        var archivo = new MockMultipartFile(
                "archivo", "documento.pdf", "application/pdf", new byte[]{1});

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> productoService.subirImagen(1L, archivo))
                .isInstanceOf(EcommerceException.class)
                .hasMessageContaining("Formato de imagen no soportado");
        verify(imagenProductoRepository, never()).save(any());
    }

    @Test
    void subirImagen_archivoVacio_lanzaEcommerceException() {
        Producto producto = Producto.builder().id(1L).activo(true).build();
        var archivo = new MockMultipartFile("archivo", "vacio.png", "image/png", new byte[0]);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> productoService.subirImagen(1L, archivo))
                .isInstanceOf(EcommerceException.class);
    }

    @Test
    void subirImagen_masDe2MB_lanzaEcommerceException() {
        Producto producto = Producto.builder().id(1L).activo(true).build();
        var archivo = new MockMultipartFile(
                "archivo", "grande.png", "image/png", new byte[2 * 1024 * 1024 + 1]);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> productoService.subirImagen(1L, archivo))
                .isInstanceOf(EcommerceException.class)
                .hasMessageContaining("2 MB");
    }

    // ─── listarTodos — búsqueda sin tildes ───────────────────────────────────

    @Test
    void listarTodos_busquedaConTildesYMayusculas_buscaNormalizado() {
        when(productoRepository.buscarConFiltros(any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());

        productoService.listarTodos(null, null, null, "  PROTEÍNA Óxido ", false, PageRequest.of(0, 10));

        verify(productoRepository).buscarConFiltros(null, null, null, "proteina oxido", false, PageRequest.of(0, 10));
    }

    @Test
    void listarTodos_busquedaConEnie_laMantiene() {
        when(productoRepository.buscarConFiltros(any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());

        productoService.listarTodos(null, null, null, "Ñandú", false, PageRequest.of(0, 10));

        verify(productoRepository).buscarConFiltros(null, null, null, "ñandu", false, PageRequest.of(0, 10));
    }

    @Test
    void listarTodos_busquedaEnPlural_buscaCadaPalabraEnSingular() {
        when(productoRepository.buscarConFiltros(any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());

        productoService.listarTodos(null, null, null, "Proteínas   Veganas", false, PageRequest.of(0, 10));

        verify(productoRepository).buscarConFiltros(null, null, null, "proteina vegana", false, PageRequest.of(0, 10));
    }

    @Test
    void listarTodos_palabraCortaTerminadaEnS_noLaCambia() {
        when(productoRepository.buscarConFiltros(any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());

        productoService.listarTodos(null, null, null, "mas BCAAs", false, PageRequest.of(0, 10));

        verify(productoRepository).buscarConFiltros(null, null, null, "mas bcaa", false, PageRequest.of(0, 10));
    }

    @Test
    void listarTodos_sinBusqueda_buscaTextoVacio() {
        when(productoRepository.buscarConFiltros(any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());

        productoService.listarTodos(null, null, null, "   ", false, PageRequest.of(0, 10));

        verify(productoRepository).buscarConFiltros(null, null, null, "", false, PageRequest.of(0, 10));
    }

    @Test
    void obtenerImagen_sinImagenCargada_lanzaNotFound() {
        when(imagenProductoRepository.findByProductoId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.obtenerImagen(1L))
                .isInstanceOf(EntidadNoEncontradaException.class);
    }

    @Test
    void eliminarImagen_borraYDesmarcaElProducto() {
        Producto producto = Producto.builder().id(1L).activo(true).tieneImagen(true).build();
        ImagenProducto imagen = ImagenProducto.builder().id(7L).producto(producto).build();

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(imagenProductoRepository.findByProductoId(1L)).thenReturn(Optional.of(imagen));

        productoService.eliminarImagen(1L);

        assertThat(producto.isTieneImagen()).isFalse();
        verify(imagenProductoRepository).delete(imagen);
    }
}
