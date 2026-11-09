package com.BlandiArruti.E_commerce.resena.service;

import com.BlandiArruti.E_commerce.administrador.entity.Administrador;
import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.repository.ClienteRepository;
import com.BlandiArruti.E_commerce.exception.DuplicadoException;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.pedido.repository.ItemPedidoRepository;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import com.BlandiArruti.E_commerce.producto.repository.ProductoRepository;
import com.BlandiArruti.E_commerce.resena.dto.request.ResenaRequest;
import com.BlandiArruti.E_commerce.resena.dto.response.PermisoResenaResponse;
import com.BlandiArruti.E_commerce.resena.dto.response.ResenaResponse;
import com.BlandiArruti.E_commerce.resena.entity.Resena;
import com.BlandiArruti.E_commerce.resena.repository.ResenaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResenaServiceTest {

    @Mock ResenaRepository resenaRepository;
    @Mock ProductoRepository productoRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock ItemPedidoRepository itemPedidoRepository;

    @InjectMocks
    ResenaService resenaService;

    private static final Long ID_PRODUCTO = 1L;
    private static final Long ID_CLIENTE = 7L;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Cliente cliente() {
        return Cliente.builder()
                .id(ID_CLIENTE)
                .nombre("María")
                .apellido("González")
                .email("maria@test.com")
                .contrasenia("hash")
                .activo(true)
                .build();
    }

    private Producto producto() {
        return Producto.builder().id(ID_PRODUCTO).nombre("Whey Protein").activo(true).build();
    }

    private void autenticarComoCliente() {
        UsuarioDetails details = UsuarioDetails.fromCliente(cliente());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    private void autenticarComoAdmin() {
        UsuarioDetails details = UsuarioDetails.fromAdmin(
                Administrador.builder().id(99L).username("admin").contrasenia("hash").build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    // ─── crear ───────────────────────────────────────────────────────────────

    @Test
    void crear_clienteQueNoComproElProducto_lanzaAccesoDenegado() {
        autenticarComoCliente();
        when(productoRepository.findByIdAndActivoTrue(ID_PRODUCTO)).thenReturn(Optional.of(producto()));
        when(resenaRepository.existsByProductoIdAndClienteId(ID_PRODUCTO, ID_CLIENTE)).thenReturn(false);
        when(itemPedidoRepository.clienteComproProducto(eq(ID_CLIENTE), eq(ID_PRODUCTO), any())).thenReturn(false);

        assertThatThrownBy(() -> resenaService.crear(ID_PRODUCTO, new ResenaRequest(5, "Muy bueno")))
                .isInstanceOf(AccessDeniedException.class);
        verify(resenaRepository, never()).save(any());
    }

    @Test
    void crear_resenaDuplicada_lanzaDuplicado() {
        autenticarComoCliente();
        when(productoRepository.findByIdAndActivoTrue(ID_PRODUCTO)).thenReturn(Optional.of(producto()));
        when(resenaRepository.existsByProductoIdAndClienteId(ID_PRODUCTO, ID_CLIENTE)).thenReturn(true);

        assertThatThrownBy(() -> resenaService.crear(ID_PRODUCTO, new ResenaRequest(4, "Otra vez")))
                .isInstanceOf(DuplicadoException.class)
                .hasMessageContaining("Ya dejaste");
        verify(resenaRepository, never()).save(any());
    }

    @Test
    void crear_estrellasFueraDeRango_lanzaError() {
        assertThatThrownBy(() -> resenaService.crear(ID_PRODUCTO, new ResenaRequest(6, null)))
                .isInstanceOf(EcommerceException.class)
                .hasMessageContaining("entre 1 y 5");
        assertThatThrownBy(() -> resenaService.crear(ID_PRODUCTO, new ResenaRequest(0, null)))
                .isInstanceOf(EcommerceException.class);
        verify(resenaRepository, never()).save(any());
    }

    @Test
    void crear_admin_lanzaAccesoDenegado() {
        autenticarComoAdmin();
        when(productoRepository.findByIdAndActivoTrue(ID_PRODUCTO)).thenReturn(Optional.of(producto()));

        assertThatThrownBy(() -> resenaService.crear(ID_PRODUCTO, new ResenaRequest(5, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void crear_clienteQueCompro_guardaYRecalculaElPromedio() {
        autenticarComoCliente();
        Producto producto = producto();
        when(productoRepository.findByIdAndActivoTrue(ID_PRODUCTO)).thenReturn(Optional.of(producto));
        when(resenaRepository.existsByProductoIdAndClienteId(ID_PRODUCTO, ID_CLIENTE)).thenReturn(false);
        when(itemPedidoRepository.clienteComproProducto(eq(ID_CLIENTE), eq(ID_PRODUCTO), any())).thenReturn(true);
        when(clienteRepository.findById(ID_CLIENTE)).thenReturn(Optional.of(cliente()));
        when(resenaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(resenaRepository.promedioDeProducto(ID_PRODUCTO)).thenReturn(4.666);
        when(resenaRepository.countByProductoId(ID_PRODUCTO)).thenReturn(3L);

        ResenaResponse respuesta = resenaService.crear(ID_PRODUCTO, new ResenaRequest(5, "  Se disuelve bien  "));

        assertThat(respuesta.autor()).isEqualTo("María G.");
        assertThat(respuesta.comentario()).isEqualTo("Se disuelve bien");
        assertThat(producto.getCantidadResenas()).isEqualTo(3);
        assertThat(producto.getPromedioResenas()).isEqualTo(4.7);
        verify(productoRepository).save(producto);
    }

    // ─── permiso ─────────────────────────────────────────────────────────────

    @Test
    void permiso_sinSesion_noPuedeOpinar() {
        when(productoRepository.findByIdAndActivoTrue(ID_PRODUCTO)).thenReturn(Optional.of(producto()));

        PermisoResenaResponse permiso = resenaService.permiso(ID_PRODUCTO);

        assertThat(permiso.puedeOpinar()).isFalse();
        assertThat(permiso.motivo()).isEqualTo("SIN_SESION");
    }

    @Test
    void permiso_clienteQueYaOpino_devuelveSuResena() {
        autenticarComoCliente();
        Resena propia = Resena.builder().id(3L).cliente(cliente()).producto(producto()).estrellas(4).build();
        when(productoRepository.findByIdAndActivoTrue(ID_PRODUCTO)).thenReturn(Optional.of(producto()));
        when(resenaRepository.findByProductoIdAndClienteId(ID_PRODUCTO, ID_CLIENTE)).thenReturn(Optional.of(propia));

        PermisoResenaResponse permiso = resenaService.permiso(ID_PRODUCTO);

        assertThat(permiso.puedeOpinar()).isFalse();
        assertThat(permiso.motivo()).isEqualTo("YA_OPINO");
        assertThat(permiso.resenaPropia().estrellas()).isEqualTo(4);
    }

    // ─── eliminar ────────────────────────────────────────────────────────────

    @Test
    void eliminar_borraYDejaElProductoSinResenas() {
        Producto producto = producto();
        producto.setCantidadResenas(1);
        producto.setPromedioResenas(5.0);
        Resena resena = Resena.builder().id(3L).cliente(cliente()).producto(producto).estrellas(5).build();
        when(resenaRepository.findByIdAndProductoId(3L, ID_PRODUCTO)).thenReturn(Optional.of(resena));
        when(resenaRepository.promedioDeProducto(ID_PRODUCTO)).thenReturn(null);
        when(resenaRepository.countByProductoId(ID_PRODUCTO)).thenReturn(0L);

        resenaService.eliminar(ID_PRODUCTO, 3L);

        verify(resenaRepository).delete(resena);
        assertThat(producto.getCantidadResenas()).isZero();
        assertThat(producto.getPromedioResenas()).isZero();
    }
}
