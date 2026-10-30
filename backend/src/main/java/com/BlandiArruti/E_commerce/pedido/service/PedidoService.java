package com.BlandiArruti.E_commerce.pedido.service;

import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.entity.Direccion;
import com.BlandiArruti.E_commerce.cliente.repository.ClienteRepository;
import com.BlandiArruti.E_commerce.cliente.repository.DireccionRepository;
import com.BlandiArruti.E_commerce.envio.dto.request.EnvioRequest;
import com.BlandiArruti.E_commerce.envio.dto.request.EstadoEnvioRequest;
import com.BlandiArruti.E_commerce.envio.dto.response.EnvioResponse;
import com.BlandiArruti.E_commerce.envio.entity.Envio;
import com.BlandiArruti.E_commerce.envio.mapper.EnvioMapper;
import com.BlandiArruti.E_commerce.envio.repository.EnvioRepository;
import com.BlandiArruti.E_commerce.enums.EstadoEnvio;
import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.enums.Rol;
import com.BlandiArruti.E_commerce.enums.TipoFactura;
import com.BlandiArruti.E_commerce.exception.*;
import com.BlandiArruti.E_commerce.mercadopago.dto.response.PreferenciaResponse;
import com.BlandiArruti.E_commerce.mercadopago.service.MercadoPagoService;
import com.BlandiArruti.E_commerce.notificacion.service.NotificacionService;
import com.BlandiArruti.E_commerce.shared.dto.PageResponse;
import com.BlandiArruti.E_commerce.factura.entity.Factura;
import com.BlandiArruti.E_commerce.factura.dto.response.FacturaResponse;
import com.BlandiArruti.E_commerce.factura.mapper.FacturaMapper;
import com.BlandiArruti.E_commerce.factura.repository.FacturaRepository;
import com.BlandiArruti.E_commerce.pedido.dto.request.EstadoPedidoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.request.ItemPedidoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.request.PagoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.request.PedidoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.response.PedidoResponse;
import com.BlandiArruti.E_commerce.pedido.entity.ItemPedido;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import com.BlandiArruti.E_commerce.pedido.mapper.PedidoMapper;
import com.BlandiArruti.E_commerce.pedido.repository.PedidoRepository;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import com.BlandiArruti.E_commerce.producto.entity.Variante;
import com.BlandiArruti.E_commerce.producto.repository.ProductoRepository;
import com.BlandiArruti.E_commerce.producto.repository.VarianteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PedidoService implements IPedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final FacturaRepository facturaRepository;
    private final EnvioRepository envioRepository;
    private final DireccionRepository direccionRepository;
    private final MercadoPagoService mercadoPagoService;
    private final NotificacionService notificacionService;
    private final PedidoMapper pedidoMapper;
    private final FacturaMapper facturaMapper;
    private final EnvioMapper envioMapper;

    private UsuarioDetails getPrincipal() {
        return (UsuarioDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private void verificarPropietario(Pedido pedido) {
        UsuarioDetails principal = getPrincipal();
        if (principal.getRol() == Rol.CLIENTE && !principal.getId().equals(pedido.getCliente().getId())) {
            throw new AccessDeniedException("No tenés permiso para acceder a este pedido.");
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<PedidoResponse> listarTodos(EstadoPedido estado, Long clienteId, Pageable pageable) {
        UsuarioDetails principal = getPrincipal();

        if (principal.getRol() == Rol.CLIENTE) {
            clienteId = principal.getId();
        }

        if (estado != null && clienteId != null) {
            return PageResponse.from(pedidoRepository.findByClienteIdAndEstadoPedido(clienteId, estado, pageable).map(pedidoMapper::toResponse));
        } else if (estado != null) {
            return PageResponse.from(pedidoRepository.findByEstadoPedido(estado, pageable).map(pedidoMapper::toResponse));
        } else if (clienteId != null) {
            return PageResponse.from(pedidoRepository.findByClienteId(clienteId, pageable).map(pedidoMapper::toResponse));
        }
        return PageResponse.from(pedidoRepository.findAll(pageable).map(pedidoMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarPorId(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> EntidadNoEncontradaException.pedido(id));
        verificarPropietario(pedido);
        return pedidoMapper.toResponse(pedido);
    }

    public PedidoResponse crear(PedidoRequest request, UsuarioDetails principal) {
        Long idCliente = principal.getRol() == Rol.CLIENTE
                ? principal.getId()
                : request.idCliente();
        if (idCliente == null) {
            throw new EcommerceException("El administrador debe especificar el idCliente en el body.");
        }
        Cliente cliente = clienteRepository.findByIdAndActivoTrue(idCliente)
                .orElseThrow(() -> EntidadNoEncontradaException.cliente(idCliente));

        List<Producto> productos = new ArrayList<>();
        List<Variante> variantes = new ArrayList<>();

        for (ItemPedidoRequest itemReq : request.items()) {
            Producto producto = productoRepository.findById(itemReq.idProducto())
                    .orElseThrow(() -> EntidadNoEncontradaException.producto(itemReq.idProducto()));
            Variante variante = null;
            if (itemReq.idVariante() != null) {
                variante = varianteRepository.findById(itemReq.idVariante())
                        .orElseThrow(() -> EntidadNoEncontradaException.variante(itemReq.idVariante()));
                if (variante.getStock() < itemReq.cantidad()) {
                    throw StockInsuficienteException.stockInsuficiente(
                            itemReq.idVariante(), variante.getStock(), itemReq.cantidad());
                }
            }
            productos.add(producto);
            variantes.add(variante);
        }

        Direccion direccionEnvio = null;
        if (request.idDireccion() != null) {
            direccionEnvio = direccionRepository.findById(request.idDireccion())
                    .orElseThrow(() -> EntidadNoEncontradaException.direccion(request.idDireccion()));
            if (!direccionEnvio.getCliente().getId().equals(cliente.getId())) {
                throw new AccessDeniedException("La dirección de envío no pertenece al cliente del pedido.");
            }
        }

        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .estadoPedido(EstadoPedido.PENDIENTE_PAGO)
                .direccionEnvio(direccionEnvio)
                .build();

        for (int i = 0; i < request.items().size(); i++) {
            ItemPedidoRequest itemReq = request.items().get(i);
            ItemPedido item = ItemPedido.builder()
                    .pedido(pedido)
                    .producto(productos.get(i))
                    .variante(variantes.get(i))
                    .cantidad(itemReq.cantidad())
                    .precioProducto(productos.get(i).getPrecioConDescuento() * itemReq.cantidad())
                    .build();
            pedido.getItems().add(item);
        }

        return pedidoMapper.toResponse(pedidoRepository.save(pedido));
    }

    public void cancelar(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> EntidadNoEncontradaException.pedido(id));
        verificarPropietario(pedido);

        EstadoPedido estado = pedido.getEstadoPedido();

        if (estado == EstadoPedido.ENTREGADO || estado == EstadoPedido.CANCELADO) {
            throw PedidoNoModificableException.noCancelable(id, estado);
        }

        if (estado == EstadoPedido.PAGADO || estado == EstadoPedido.DESPACHADO) {
            for (ItemPedido item : pedido.getItems()) {
                Variante variante = item.getVariante();
                if (variante != null) {
                    variante.setStock(variante.getStock() + item.getCantidad());
                    varianteRepository.save(variante);
                }
            }
        }

        pedido.setEstadoPedido(EstadoPedido.CANCELADO);
        pedidoRepository.save(pedido);
    }

    public PreferenciaResponse iniciarPagoMercadoPago(Long id, PagoRequest request) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> EntidadNoEncontradaException.pedido(id));
        verificarPropietario(pedido);

        if (pedido.getEstadoPedido() != EstadoPedido.PENDIENTE_PAGO) {
            throw PedidoNoModificableException.porEstado(id, pedido.getEstadoPedido());
        }

        return mercadoPagoService.crearPreferencia(pedido, request.tipoFactura());
    }

    public void confirmarPagoMercadoPago(Long pedidoId, TipoFactura tipoFactura) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> EntidadNoEncontradaException.pedido(pedidoId));

        // El webhook puede llegar repetido: si el pedido ya no está esperando pago, no hacemos nada.
        if (!esCobrable(pedido.getEstadoPedido())) {
            return;
        }

        acreditarPago(pedido, tipoFactura);
    }

    public PedidoResponse pagar(Long id, PagoRequest request) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> EntidadNoEncontradaException.pedido(id));
        verificarPropietario(pedido);

        if (!esCobrable(pedido.getEstadoPedido())) {
            throw PedidoNoModificableException.porEstado(id, pedido.getEstadoPedido());
        }

        return pedidoMapper.toResponse(acreditarPago(pedido, request.tipoFactura()));
    }

    /** Estados desde los que todavía se puede cobrar un pedido. */
    private boolean esCobrable(EstadoPedido estado) {
        return estado == EstadoPedido.PENDIENTE_PAGO || estado == EstadoPedido.EN_PREPARACION;
    }

    /**
     * Único camino a PAGADO: descuenta stock, emite la factura y notifica.
     * Lo comparten el pago directo y la confirmación de MercadoPago para que
     * no puedan divergir.
     */
    private Pedido acreditarPago(Pedido pedido, TipoFactura tipoFactura) {
        for (ItemPedido item : pedido.getItems()) {
            Variante variante = item.getVariante();
            if (variante != null) {
                if (variante.getStock() < item.getCantidad()) {
                    throw StockInsuficienteException.stockInsuficiente(
                            variante.getId(), variante.getStock(), item.getCantidad());
                }
                variante.setStock(variante.getStock() - item.getCantidad());
                varianteRepository.save(variante);
            }
        }

        double total = pedido.getItems().stream()
                .mapToDouble(ItemPedido::getPrecioProducto)
                .sum();

        Factura factura = Factura.builder()
                .pedido(pedido)
                .tipoFactura(tipoFactura)
                .precioTotal(total)
                .build();
        facturaRepository.save(factura);

        pedido.setEstadoPedido(EstadoPedido.PAGADO);
        pedido.setFactura(factura);
        Pedido guardado = pedidoRepository.save(pedido);

        notificacionService.notificarFacturaGenerada(factura);

        return guardado;
    }

    public PedidoResponse cambiarEstado(Long id, EstadoPedidoRequest request) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> EntidadNoEncontradaException.pedido(id));

        EstadoPedido actual = pedido.getEstadoPedido();
        EstadoPedido nuevo = request.estado();

        // A PAGADO solo se llega cobrando (pagar / webhook de MercadoPago): ese es el
        // único camino que descuenta stock y emite factura.
        if (nuevo == EstadoPedido.PAGADO) {
            throw new EcommerceException(
                    "No se puede marcar un pedido como PAGADO a mano. "
                    + "Usá PATCH /pedido/" + id + "/pagar o el pago con MercadoPago.");
        }

        boolean valida = switch (actual) {
            case PENDIENTE_PAGO -> nuevo == EstadoPedido.EN_PREPARACION || nuevo == EstadoPedido.CANCELADO;
            case EN_PREPARACION -> nuevo == EstadoPedido.CANCELADO;
            case PAGADO         -> nuevo == EstadoPedido.DESPACHADO || nuevo == EstadoPedido.CANCELADO;
            case DESPACHADO     -> nuevo == EstadoPedido.ENTREGADO || nuevo == EstadoPedido.CANCELADO;
            default             -> false;
        };

        if (!valida) {
            throw new EcommerceException(
                    "Transición de estado inválida: " + actual + " → " + nuevo + ".");
        }

        // El stock se devuelve solo si ya había sido descontado (es decir, si el pedido fue cobrado).
        if (nuevo == EstadoPedido.CANCELADO
                && (actual == EstadoPedido.PAGADO || actual == EstadoPedido.DESPACHADO)) {
            for (ItemPedido item : pedido.getItems()) {
                Variante variante = item.getVariante();
                if (variante != null) {
                    variante.setStock(variante.getStock() + item.getCantidad());
                    varianteRepository.save(variante);
                }
            }
        }

        pedido.setEstadoPedido(nuevo);
        return pedidoMapper.toResponse(pedidoRepository.save(pedido));
    }

    @Transactional(readOnly = true)
    public FacturaResponse obtenerFactura(Long idPedido) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> EntidadNoEncontradaException.pedido(idPedido));
        verificarPropietario(pedido);
        if (pedido.getFactura() == null) {
            throw new ConflictoException("El pedido con id " + idPedido + " aún no tiene factura.");
        }
        return facturaMapper.toResponse(pedido.getFactura());
    }

    @Transactional(readOnly = true)
    public EnvioResponse obtenerEnvio(Long idPedido) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> EntidadNoEncontradaException.pedido(idPedido));
        verificarPropietario(pedido);
        Envio envio = envioRepository.findByPedidoId(idPedido)
                .orElseThrow(() -> EntidadNoEncontradaException.envio(idPedido));
        return envioMapper.toResponse(envio);
    }

    public EnvioResponse crearEnvio(Long idPedido, EnvioRequest request) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> EntidadNoEncontradaException.pedido(idPedido));

        if (pedido.getEstadoPedido() != EstadoPedido.PAGADO) {
            throw new EcommerceException(
                    "Solo se puede crear un envío para pedidos en estado PAGADO.");
        }

        if (envioRepository.findByPedidoId(idPedido).isPresent()) {
            throw new ConflictoException("El pedido con id " + idPedido + " ya tiene un envío asignado.");
        }

        Direccion direccion = pedido.getDireccionEnvio();
        if (direccion == null) {
            throw new EcommerceException("El pedido no tiene dirección de envío. El cliente debe indicarla al hacer el checkout.");
        }

        Envio envio = Envio.builder()
                .pedido(pedido)
                .direccion(direccion)
                .estado(EstadoEnvio.DESPACHADO)
                .fechaSalida(request.fechaSalida())
                .fechaLlegada(request.fechaLlegada())
                .build();
        envioRepository.save(envio);

        pedido.setEstadoPedido(EstadoPedido.DESPACHADO);
        pedido.setEnvio(envio);
        pedidoRepository.save(pedido);

        notificacionService.notificarEnvioActualizado(envio);

        return envioMapper.toResponse(envio);
    }

    public EnvioResponse actualizarEstadoEnvio(Long idPedido, EstadoEnvioRequest request) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> EntidadNoEncontradaException.pedido(idPedido));
        Envio envio = envioRepository.findByPedidoId(idPedido)
                .orElseThrow(() -> EntidadNoEncontradaException.envio(idPedido));

        EstadoEnvio actual = envio.getEstado();
        EstadoEnvio nuevo = request.estado();

        boolean valida = switch (actual) {
            case DESPACHADO -> nuevo == EstadoEnvio.EN_CAMINO;
            case EN_CAMINO  -> nuevo == EstadoEnvio.ENTREGADO;
            default         -> false;
        };

        if (!valida) {
            throw new EcommerceException(
                    "Transición de estado de envío inválida: " + actual + " → " + nuevo + ".");
        }

        envio.setEstado(nuevo);

        if (nuevo == EstadoEnvio.ENTREGADO) {
            pedido.setEstadoPedido(EstadoPedido.ENTREGADO);
            pedidoRepository.save(pedido);
        }

        envioRepository.save(envio);
        notificacionService.notificarEnvioActualizado(envio);

        return envioMapper.toResponse(envio);
    }
}
