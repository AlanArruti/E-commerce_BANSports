package com.BlandiArruti.E_commerce.resena.service;

import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.repository.ClienteRepository;
import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.enums.Rol;
import com.BlandiArruti.E_commerce.exception.DuplicadoException;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.pedido.repository.ItemPedidoRepository;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import com.BlandiArruti.E_commerce.producto.repository.ProductoRepository;
import com.BlandiArruti.E_commerce.resena.dto.request.ResenaRequest;
import com.BlandiArruti.E_commerce.resena.dto.response.PermisoResenaResponse;
import com.BlandiArruti.E_commerce.resena.dto.response.ResenaResponse;
import com.BlandiArruti.E_commerce.resena.entity.Resena;
import com.BlandiArruti.E_commerce.resena.repository.ResenaRepository;
import com.BlandiArruti.E_commerce.shared.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class ResenaService {

    /** Puede opinar quien tiene el producto en un pedido pagado o más avanzado (no pendiente ni cancelado). */
    static final Set<EstadoPedido> ESTADOS_CON_COMPRA = EnumSet.of(
            EstadoPedido.PAGADO, EstadoPedido.EN_PREPARACION, EstadoPedido.DESPACHADO, EstadoPedido.ENTREGADO);

    private final ResenaRepository resenaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final ItemPedidoRepository itemPedidoRepository;

    /** Reseñas del producto, públicas y paginadas. */
    @Transactional(readOnly = true)
    public PageResponse<ResenaResponse> listar(Long idProducto, Pageable pageable) {
        buscarProductoActivo(idProducto);
        return PageResponse.from(resenaRepository.findByProductoId(idProducto, pageable).map(this::toResponse));
    }

    /** Si quien consulta (con o sin sesión) puede dejar una reseña, y por qué no. */
    @Transactional(readOnly = true)
    public PermisoResenaResponse permiso(Long idProducto) {
        buscarProductoActivo(idProducto);
        UsuarioDetails usuario = usuarioActual();
        if (usuario == null) {
            return new PermisoResenaResponse(false, "SIN_SESION", null);
        }
        if (usuario.getRol() != Rol.CLIENTE) {
            return new PermisoResenaResponse(false, "NO_ES_CLIENTE", null);
        }
        var propia = resenaRepository.findByProductoIdAndClienteId(idProducto, usuario.getId());
        if (propia.isPresent()) {
            return new PermisoResenaResponse(false, "YA_OPINO", toResponse(propia.get()));
        }
        if (!itemPedidoRepository.clienteComproProducto(usuario.getId(), idProducto, ESTADOS_CON_COMPRA)) {
            return new PermisoResenaResponse(false, "NO_LO_COMPRO", null);
        }
        return new PermisoResenaResponse(true, "OK", null);
    }

    /** Solo un cliente que compró el producto, y una sola vez por producto. */
    public ResenaResponse crear(Long idProducto, ResenaRequest request) {
        if (request.estrellas() == null || request.estrellas() < 1 || request.estrellas() > 5) {
            throw new EcommerceException("Las estrellas tienen que ser entre 1 y 5.");
        }
        Producto producto = buscarProductoActivo(idProducto);
        UsuarioDetails usuario = usuarioActual();
        if (usuario == null || usuario.getRol() != Rol.CLIENTE) {
            throw new AccessDeniedException("Solo los clientes pueden dejar reseñas.");
        }
        if (resenaRepository.existsByProductoIdAndClienteId(idProducto, usuario.getId())) {
            throw new DuplicadoException("Ya dejaste una reseña para este producto.");
        }
        if (!itemPedidoRepository.clienteComproProducto(usuario.getId(), idProducto, ESTADOS_CON_COMPRA)) {
            throw new AccessDeniedException("Solo pueden opinar quienes compraron este producto.");
        }
        Cliente cliente = clienteRepository.findById(usuario.getId())
                .orElseThrow(() -> EntidadNoEncontradaException.cliente(usuario.getId()));

        String comentario = request.comentario() == null || request.comentario().isBlank()
                ? null
                : request.comentario().trim();
        Resena resena = resenaRepository.save(Resena.builder()
                .producto(producto)
                .cliente(cliente)
                .estrellas(request.estrellas())
                .comentario(comentario)
                .build());

        actualizarResumen(producto);
        return toResponse(resena);
    }

    /** Moderación (solo ADMIN, lo controla SecurityConfig). */
    public void eliminar(Long idProducto, Long idResena) {
        Resena resena = resenaRepository.findByIdAndProductoId(idResena, idProducto)
                .orElseThrow(() -> new EntidadNoEncontradaException(
                        "Reseña con id " + idResena + " no encontrada en el producto " + idProducto + "."));
        Producto producto = resena.getProducto();
        resenaRepository.delete(resena);
        resenaRepository.flush();
        actualizarResumen(producto);
    }

    /** Recalcula el promedio y la cantidad que viajan en ProductoResponse. */
    private void actualizarResumen(Producto producto) {
        Double promedio = resenaRepository.promedioDeProducto(producto.getId());
        producto.setCantidadResenas((int) resenaRepository.countByProductoId(producto.getId()));
        producto.setPromedioResenas(promedio == null
                ? 0.0
                : BigDecimal.valueOf(promedio).setScale(1, RoundingMode.HALF_UP).doubleValue());
        productoRepository.save(producto);
    }

    private Producto buscarProductoActivo(Long idProducto) {
        return productoRepository.findByIdAndActivoTrue(idProducto)
                .orElseThrow(() -> EntidadNoEncontradaException.producto(idProducto));
    }

    /** El usuario logueado, o null si la consulta llega sin sesión (los GET son públicos). */
    private UsuarioDetails usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof UsuarioDetails usuario ? usuario : null;
    }

    private ResenaResponse toResponse(Resena resena) {
        Cliente cliente = resena.getCliente();
        String inicial = cliente.getApellido() == null || cliente.getApellido().isBlank()
                ? ""
                : " " + cliente.getApellido().trim().charAt(0) + ".";
        return new ResenaResponse(
                resena.getId(),
                cliente.getNombre() + inicial,
                resena.getEstrellas(),
                resena.getComentario(),
                resena.getFecha());
    }
}
