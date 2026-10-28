package com.BlandiArruti.E_commerce.carrito.service;

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
import com.BlandiArruti.E_commerce.enums.Rol;
import com.BlandiArruti.E_commerce.exception.ConflictoException;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.exception.StockInsuficienteException;
import com.BlandiArruti.E_commerce.pedido.dto.request.ItemPedidoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.request.PedidoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.response.PedidoResponse;
import com.BlandiArruti.E_commerce.pedido.service.IPedidoService;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import com.BlandiArruti.E_commerce.producto.entity.Variante;
import com.BlandiArruti.E_commerce.producto.repository.ProductoRepository;
import com.BlandiArruti.E_commerce.producto.repository.VarianteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final ItemCarritoRepository itemCarritoRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final CarritoMapper carritoMapper;
    private final IPedidoService pedidoService;

    @Transactional(readOnly = true)
    public CarritoResponse obtenerCarrito(Long idCliente) {
        return carritoMapper.toResponse(obtenerOCrearCarrito(idCliente));
    }

    public CarritoResponse agregarItem(Long idCliente, ItemCarritoRequest request) {
        Carrito carrito = obtenerOCrearCarrito(idCliente);

        Variante variante;
        Producto producto;

        if (request.idVariante() != null) {
            variante = varianteRepository.findByIdAndActivoTrue(request.idVariante())
                    .orElseThrow(() -> EntidadNoEncontradaException.variante(request.idVariante()));
            producto = variante.getProducto();
        } else {
            producto = productoRepository.findByIdAndActivoTrue(request.idProducto())
                    .orElseThrow(() -> EntidadNoEncontradaException.producto(request.idProducto()));
            List<Variante> variantes = varianteRepository.findByProductoIdAndActivoTrue(request.idProducto());
            if (variantes.size() > 1) {
                throw new ConflictoException(
                        "El producto '" + producto.getNombre() + "' tiene múltiples variantes. Indicá el idVariante.");
            }
            variante = variantes.isEmpty() ? null : variantes.get(0);
        }

        ItemCarrito item = (variante != null)
                ? itemCarritoRepository.findByCarritoIdAndVarianteId(carrito.getId(), variante.getId()).orElse(null)
                : itemCarritoRepository.findByCarritoIdAndProductoIdAndVarianteIsNull(carrito.getId(), producto.getId()).orElse(null);

        int cantidadFinal = (item != null ? item.getCantidad() : 0) + request.cantidad();

        if (variante != null) {
            validarStock(variante, cantidadFinal);
        }

        if (item != null) {
            item.setCantidad(cantidadFinal);
        } else {
            item = ItemCarrito.builder()
                    .carrito(carrito)
                    .producto(producto)
                    .variante(variante)
                    .cantidad(cantidadFinal)
                    .build();
            carrito.getItems().add(item);
        }
        itemCarritoRepository.save(item);

        return carritoMapper.toResponse(carrito);
    }

    public CarritoResponse actualizarCantidad(Long idCliente, Long idItem, CantidadCarritoRequest request) {
        Carrito carrito = obtenerOCrearCarrito(idCliente);
        ItemCarrito item = itemCarritoRepository.findByIdAndCarritoId(idItem, carrito.getId())
                .orElseThrow(() -> EntidadNoEncontradaException.itemCarrito(idItem));

        if (item.getVariante() != null) {
            validarStock(item.getVariante(), request.cantidad());
        }

        item.setCantidad(request.cantidad());
        itemCarritoRepository.save(item);

        return carritoMapper.toResponse(carrito);
    }

    public CarritoResponse eliminarItem(Long idCliente, Long idItem) {
        Carrito carrito = obtenerOCrearCarrito(idCliente);
        ItemCarrito item = itemCarritoRepository.findByIdAndCarritoId(idItem, carrito.getId())
                .orElseThrow(() -> EntidadNoEncontradaException.itemCarrito(idItem));

        carrito.getItems().remove(item);
        itemCarritoRepository.delete(item);

        return carritoMapper.toResponse(carrito);
    }

    public void vaciarCarrito(Long idCliente) {
        Carrito carrito = obtenerOCrearCarrito(idCliente);
        carrito.getItems().clear();
        carritoRepository.save(carrito);
    }

    public PedidoResponse checkout(Long idCliente, Long idDireccion) {
        Carrito carrito = obtenerOCrearCarrito(idCliente);

        if (carrito.getItems().isEmpty()) {
            throw new ConflictoException("El carrito está vacío.");
        }

        List<ItemPedidoRequest> items = carrito.getItems().stream()
                .map(item -> new ItemPedidoRequest(
                        item.getProducto().getId(),
                        item.getVariante() != null ? item.getVariante().getId() : null,
                        item.getCantidad()))
                .toList();

        PedidoResponse pedido = pedidoService.crear(new PedidoRequest(idCliente, idDireccion, items), getPrincipal());

        carrito.getItems().clear();
        carritoRepository.save(carrito);

        return pedido;
    }

    private UsuarioDetails getPrincipal() {
        return (UsuarioDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private void verificarPropietario(Long idCliente) {
        UsuarioDetails principal = getPrincipal();
        if (principal.getRol() == Rol.CLIENTE && !principal.getId().equals(idCliente)) {
            throw new AccessDeniedException("No tenés permiso para acceder a este carrito.");
        }
    }

    private Carrito obtenerOCrearCarrito(Long idCliente) {
        verificarPropietario(idCliente);
        return carritoRepository.findByClienteId(idCliente)
                .orElseGet(() -> {
                    Cliente cliente = clienteRepository.findById(idCliente)
                            .orElseThrow(() -> EntidadNoEncontradaException.cliente(idCliente));
                    Carrito nuevo = Carrito.builder().cliente(cliente).build();
                    return carritoRepository.save(nuevo);
                });
    }

    private void validarStock(Variante variante, int cantidadSolicitada) {
        if (variante.getStock() < cantidadSolicitada) {
            throw StockInsuficienteException.stockInsuficiente(
                    variante.getId(), variante.getStock(), cantidadSolicitada);
        }
    }
}
