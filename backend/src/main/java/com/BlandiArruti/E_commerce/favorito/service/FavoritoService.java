package com.BlandiArruti.E_commerce.favorito.service;

import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.repository.ClienteRepository;
import com.BlandiArruti.E_commerce.enums.Rol;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.favorito.dto.response.FavoritoResponse;
import com.BlandiArruti.E_commerce.favorito.entity.Favorito;
import com.BlandiArruti.E_commerce.favorito.repository.FavoritoRepository;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import com.BlandiArruti.E_commerce.producto.mapper.ProductoMapper;
import com.BlandiArruti.E_commerce.producto.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;

    /** Favoritos del cliente, del más nuevo al más viejo, sin los productos dados de baja. */
    @Transactional(readOnly = true)
    public List<FavoritoResponse> listar(Long idCliente) {
        verificarPropietario(idCliente);
        return favoritoRepository.findByClienteIdAndProductoActivoTrueOrderByFechaAgregadoDesc(idCliente)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Guarda el producto en favoritos. Si ya estaba, devuelve el que existe (no duplica). */
    public FavoritoResponse agregar(Long idCliente, Long idProducto) {
        verificarPropietario(idCliente);
        Producto producto = productoRepository.findByIdAndActivoTrue(idProducto)
                .orElseThrow(() -> EntidadNoEncontradaException.producto(idProducto));

        Favorito favorito = favoritoRepository.findByClienteIdAndProductoId(idCliente, idProducto)
                .orElseGet(() -> {
                    Cliente cliente = clienteRepository.findById(idCliente)
                            .orElseThrow(() -> EntidadNoEncontradaException.cliente(idCliente));
                    return favoritoRepository.save(
                            Favorito.builder().cliente(cliente).producto(producto).build());
                });
        return toResponse(favorito);
    }

    /** Quita el producto de favoritos. Si no estaba, no hace nada. */
    public void quitar(Long idCliente, Long idProducto) {
        verificarPropietario(idCliente);
        favoritoRepository.findByClienteIdAndProductoId(idCliente, idProducto)
                .ifPresent(favoritoRepository::delete);
    }

    private FavoritoResponse toResponse(Favorito favorito) {
        return new FavoritoResponse(productoMapper.toResponse(favorito.getProducto()), favorito.getFechaAgregado());
    }

    private void verificarPropietario(Long idCliente) {
        UsuarioDetails principal = (UsuarioDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        if (principal.getRol() == Rol.CLIENTE && !principal.getId().equals(idCliente)) {
            throw new AccessDeniedException("No tenés permiso para acceder a estos favoritos.");
        }
    }
}
