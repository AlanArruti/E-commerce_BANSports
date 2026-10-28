package com.BlandiArruti.E_commerce.carrito.service;

import com.BlandiArruti.E_commerce.administrador.entity.Administrador;
import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.carrito.dto.request.CantidadCarritoRequest;
import com.BlandiArruti.E_commerce.carrito.dto.request.ItemCarritoRequest;
import com.BlandiArruti.E_commerce.carrito.dto.response.CarritoResponse;
import com.BlandiArruti.E_commerce.carrito.entity.Carrito;
import com.BlandiArruti.E_commerce.carrito.entity.ItemCarrito;
import com.BlandiArruti.E_commerce.carrito.mapper.CarritoMapper;
import com.BlandiArruti.E_commerce.carrito.repository.CarritoRepository;
import com.BlandiArruti.E_commerce.carrito.repository.ItemCarritoRepository;
import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.repository.ClienteRepository;
import com.BlandiArruti.E_commerce.exception.ConflictoException;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.exception.StockInsuficienteException;
import com.BlandiArruti.E_commerce.pedido.service.IPedidoService;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import com.BlandiArruti.E_commerce.producto.entity.Variante;
import com.BlandiArruti.E_commerce.producto.repository.ProductoRepository;
import com.BlandiArruti.E_commerce.producto.repository.VarianteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarritoServiceTest {

    @Mock CarritoRepository carritoRepository;
    @Mock ItemCarritoRepository itemCarritoRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock ProductoRepository productoRepository;
    @Mock VarianteRepository varianteRepository;
    @Mock CarritoMapper carritoMapper;
    @Mock IPedidoService pedidoService;

    @InjectMocks
    CarritoService carritoService;

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

    private Carrito carritoVacio(Long idCliente) {
        Cliente cliente = Cliente.builder().id(idCliente).build();
        return Carrito.builder().id(1L).cliente(cliente).build();
    }

    private Variante varianteConStock(Long idProducto, int stock) {
        Producto producto = Producto.builder().id(idProducto).nombre("Producto " + idProducto).build();
        return Variante.builder()
                .id(idProducto)
                .producto(producto)
                .stock(stock)
                .activo(true)
                .build();
    }

    private CarritoResponse responseVacio() {
        return new CarritoResponse(1L, "uuid-1", List.of(), 0.0);
    }

    // ─── obtenerCarrito() ────────────────────────────────────────────────────

    @Test
    void obtenerCarrito_carritoExistenteEnBD_retornaResponse() {
        autenticarComoCliente(1L);
        Carrito carrito = carritoVacio(1L);
        when(carritoRepository.findByClienteId(1L)).thenReturn(Optional.of(carrito));
        when(carritoMapper.toResponse(carrito)).thenReturn(responseVacio());

        CarritoResponse result = carritoService.obtenerCarrito(1L);

        assertThat(result).isNotNull();
        verify(clienteRepository, never()).findById(any());
    }

    @Test
    void obtenerCarrito_sinCarritoEnBD_creaUnoNuevoParaElCliente() {
        autenticarComoCliente(1L);
        Cliente cliente = Cliente.builder().id(1L).build();
        Carrito nuevoCarrito = carritoVacio(1L);
        when(carritoRepository.findByClienteId(1L)).thenReturn(Optional.empty());
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(carritoRepository.save(any())).thenReturn(nuevoCarrito);
        when(carritoMapper.toResponse(nuevoCarrito)).thenReturn(responseVacio());

        carritoService.obtenerCarrito(1L);

        verify(carritoRepository).save(any(Carrito.class));
    }

    @Test
    void obtenerCarrito_clienteAccediendoAOtroCarrito_lanzaAccessDenied() {
        autenticarComoCliente(1L);

        assertThatThrownBy(() -> carritoService.obtenerCarrito(2L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void obtenerCarrito_admin_puedeVerCarritoDeOtroCliente() {
        autenticarComoAdmin();
        Carrito carrito = carritoVacio(5L);
        when(carritoRepository.findByClienteId(5L)).thenReturn(Optional.of(carrito));
        when(carritoMapper.toResponse(carrito)).thenReturn(responseVacio());

        CarritoResponse result = carritoService.obtenerCarrito(5L);

        assertThat(result).isNotNull();
    }

    // ─── agregarItem() ───────────────────────────────────────────────────────

    @Test
    void agregarItem_itemNuevo_agreaItemAlCarrito() {
        autenticarComoCliente(1L);
        Carrito carrito = carritoVacio(1L);
        Variante variante = varianteConStock(10L, 5);
        ItemCarritoRequest request = new ItemCarritoRequest(10L, 10L, 2);

        when(carritoRepository.findByClienteId(1L)).thenReturn(Optional.of(carrito));
        when(varianteRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(variante));
        when(itemCarritoRepository.findByCarritoIdAndVarianteId(1L, 10L)).thenReturn(Optional.empty());
        when(itemCarritoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(carritoMapper.toResponse(carrito)).thenReturn(responseVacio());

        carritoService.agregarItem(1L, request);

        assertThat(carrito.getItems()).hasSize(1);
        verify(itemCarritoRepository).save(any(ItemCarrito.class));
    }

    @Test
    void agregarItem_itemExistente_sumaCantidades() {
        autenticarComoCliente(1L);
        Variante variante = varianteConStock(10L, 10);
        Carrito carrito = carritoVacio(1L);
        ItemCarrito itemExistente = ItemCarrito.builder()
                .id(1L).carrito(carrito).variante(variante)
                .producto(variante.getProducto()).cantidad(2).build();
        carrito.getItems().add(itemExistente);

        when(carritoRepository.findByClienteId(1L)).thenReturn(Optional.of(carrito));
        when(varianteRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(variante));
        when(itemCarritoRepository.findByCarritoIdAndVarianteId(1L, 10L))
                .thenReturn(Optional.of(itemExistente));
        when(itemCarritoRepository.save(itemExistente)).thenReturn(itemExistente);
        when(carritoMapper.toResponse(carrito)).thenReturn(responseVacio());

        carritoService.agregarItem(1L, new ItemCarritoRequest(10L, 10L, 3));

        assertThat(itemExistente.getCantidad()).isEqualTo(5);
    }

    @Test
    void agregarItem_stockInsuficiente_lanzaStockInsuficienteException() {
        autenticarComoCliente(1L);
        Variante variante = varianteConStock(10L, 1);

        when(carritoRepository.findByClienteId(1L)).thenReturn(Optional.of(carritoVacio(1L)));
        when(varianteRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(variante));
        when(itemCarritoRepository.findByCarritoIdAndVarianteId(any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> carritoService.agregarItem(1L, new ItemCarritoRequest(10L, 10L, 5)))
                .isInstanceOf(StockInsuficienteException.class);
    }

    @Test
    void agregarItem_varianteInexistente_lanzaEntidadNoEncontrada() {
        autenticarComoCliente(1L);
        when(carritoRepository.findByClienteId(1L)).thenReturn(Optional.of(carritoVacio(1L)));
        when(varianteRepository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> carritoService.agregarItem(1L, new ItemCarritoRequest(1L, 99L, 1)))
                .isInstanceOf(EntidadNoEncontradaException.class);
    }

    @Test
    void agregarItem_sinIdVariante_autoSeleccionaUnicaVarianteDelProducto() {
        autenticarComoCliente(1L);
        Carrito carrito = carritoVacio(1L);
        Variante variante = varianteConStock(10L, 5);
        Producto producto = variante.getProducto();

        when(carritoRepository.findByClienteId(1L)).thenReturn(Optional.of(carrito));
        when(productoRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(producto));
        when(varianteRepository.findByProductoIdAndActivoTrue(10L)).thenReturn(List.of(variante));
        when(itemCarritoRepository.findByCarritoIdAndVarianteId(1L, 10L)).thenReturn(Optional.empty());
        when(itemCarritoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(carritoMapper.toResponse(carrito)).thenReturn(responseVacio());

        carritoService.agregarItem(1L, new ItemCarritoRequest(10L, null, 2));

        assertThat(carrito.getItems()).hasSize(1);
        assertThat(carrito.getItems().get(0).getVariante()).isEqualTo(variante);
    }

    @Test
    void agregarItem_sinIdVariante_productoSinVariantes_agrega_con_variante_nula() {
        autenticarComoCliente(1L);
        Carrito carrito = carritoVacio(1L);
        Producto producto = Producto.builder().id(10L).nombre("Smartphone").activo(true).build();

        when(carritoRepository.findByClienteId(1L)).thenReturn(Optional.of(carrito));
        when(productoRepository.findByIdAndActivoTrue(10L)).thenReturn(Optional.of(producto));
        when(varianteRepository.findByProductoIdAndActivoTrue(10L)).thenReturn(List.of());
        when(itemCarritoRepository.findByCarritoIdAndProductoIdAndVarianteIsNull(1L, 10L)).thenReturn(Optional.empty());
        when(itemCarritoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(carritoMapper.toResponse(carrito)).thenReturn(responseVacio());

        carritoService.agregarItem(1L, new ItemCarritoRequest(10L, null, 1));

        assertThat(carrito.getItems()).hasSize(1);
        assertThat(carrito.getItems().get(0).getVariante()).isNull();
    }

    // ─── actualizarCantidad() ────────────────────────────────────────────────

    @Test
    void actualizarCantidad_itemExistente_cambiaCantidad() {
        autenticarComoCliente(1L);
        Variante variante = varianteConStock(10L, 10);
        Carrito carrito = carritoVacio(1L);
        ItemCarrito item = ItemCarrito.builder()
                .id(5L).carrito(carrito).variante(variante)
                .producto(variante.getProducto()).cantidad(2).build();

        when(carritoRepository.findByClienteId(1L)).thenReturn(Optional.of(carrito));
        when(itemCarritoRepository.findByIdAndCarritoId(5L, 1L)).thenReturn(Optional.of(item));
        when(itemCarritoRepository.save(item)).thenReturn(item);
        when(carritoMapper.toResponse(carrito)).thenReturn(responseVacio());

        carritoService.actualizarCantidad(1L, 5L, new CantidadCarritoRequest(7));

        assertThat(item.getCantidad()).isEqualTo(7);
    }

    // ─── eliminarItem() ──────────────────────────────────────────────────────

    @Test
    void eliminarItem_itemExistente_eliminaDelCarrito() {
        autenticarComoCliente(1L);
        Variante variante = varianteConStock(10L, 10);
        Carrito carrito = carritoVacio(1L);
        ItemCarrito item = ItemCarrito.builder()
                .id(5L).carrito(carrito).variante(variante)
                .producto(variante.getProducto()).cantidad(1).build();
        carrito.getItems().add(item);

        when(carritoRepository.findByClienteId(1L)).thenReturn(Optional.of(carrito));
        when(itemCarritoRepository.findByIdAndCarritoId(5L, 1L)).thenReturn(Optional.of(item));
        when(carritoMapper.toResponse(carrito)).thenReturn(responseVacio());

        carritoService.eliminarItem(1L, 5L);

        assertThat(carrito.getItems()).isEmpty();
        verify(itemCarritoRepository).delete(item);
    }

    // ─── vaciarCarrito() ─────────────────────────────────────────────────────

    @Test
    void vaciarCarrito_carritoConItems_eliminaTodosLosItems() {
        autenticarComoCliente(1L);
        Carrito carrito = carritoVacio(1L);
        carrito.getItems().add(ItemCarrito.builder().id(1L).build());
        carrito.getItems().add(ItemCarrito.builder().id(2L).build());

        when(carritoRepository.findByClienteId(1L)).thenReturn(Optional.of(carrito));

        carritoService.vaciarCarrito(1L);

        assertThat(carrito.getItems()).isEmpty();
        verify(carritoRepository).save(carrito);
    }

    // ─── checkout() ──────────────────────────────────────────────────────────

    @Test
    void checkout_carritoVacio_lanzaConflictoException() {
        autenticarComoCliente(1L);
        Carrito carrito = carritoVacio(1L);
        when(carritoRepository.findByClienteId(1L)).thenReturn(Optional.of(carrito));

        assertThatThrownBy(() -> carritoService.checkout(1L, null))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    void checkout_carritoConItems_creaPedidoYVaciaCarrito() {
        autenticarComoCliente(1L);
        Variante variante = varianteConStock(10L, 5);
        Carrito carrito = carritoVacio(1L);
        ItemCarrito item = ItemCarrito.builder()
                .id(1L).carrito(carrito)
                .producto(variante.getProducto()).variante(variante).cantidad(2).build();
        carrito.getItems().add(item);

        when(carritoRepository.findByClienteId(1L)).thenReturn(Optional.of(carrito));
        when(pedidoService.crear(any(), any())).thenReturn(null);

        carritoService.checkout(1L, null);

        assertThat(carrito.getItems()).isEmpty();
        verify(carritoRepository).save(carrito);
    }
}
