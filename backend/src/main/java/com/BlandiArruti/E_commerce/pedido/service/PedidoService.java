package com.BlandiArruti.E_commerce.pedido.service;

import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.entity.Direccion;
import com.BlandiArruti.E_commerce.carrito.repository.CarritoRepository;
import com.BlandiArruti.E_commerce.cliente.repository.ClienteRepository;
import com.BlandiArruti.E_commerce.cliente.repository.DireccionRepository;
import com.BlandiArruti.E_commerce.cupon.service.ICuponService;
import com.BlandiArruti.E_commerce.cotizacion.service.CotizacionEnvioService;
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
import com.BlandiArruti.E_commerce.enums.EstadoReembolso;
import com.BlandiArruti.E_commerce.enums.TipoReembolso;
import com.BlandiArruti.E_commerce.pedido.dto.request.ResolucionReembolsoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.request.SolicitudReembolsoRequest;
import com.BlandiArruti.E_commerce.pedido.dto.response.CancelacionPedidoResponse;
import com.BlandiArruti.E_commerce.exception.*;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import com.BlandiArruti.E_commerce.notificacion.service.NotificacionClienteService;
import com.BlandiArruti.E_commerce.notificacion.service.NotificacionService;
import com.BlandiArruti.E_commerce.shared.dto.PageResponse;
import com.BlandiArruti.E_commerce.factura.entity.Factura;
import com.BlandiArruti.E_commerce.factura.dto.response.FacturaPdfResponse;
import com.BlandiArruti.E_commerce.factura.dto.response.FacturaResponse;
import com.BlandiArruti.E_commerce.factura.pdf.FacturaPdfGenerator;
import com.BlandiArruti.E_commerce.factura.mapper.FacturaMapper;
import com.BlandiArruti.E_commerce.factura.repository.FacturaRepository;
import com.BlandiArruti.E_commerce.enums.ConfirmacionRecepcion;
import com.BlandiArruti.E_commerce.pedido.dto.request.ConfirmarRecepcionRequest;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PedidoService implements IPedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final FacturaRepository facturaRepository;
    private final EnvioRepository envioRepository;
    private final DireccionRepository direccionRepository;
    private final NotificacionService notificacionService;
    private final PedidoMapper pedidoMapper;
    private final FacturaMapper facturaMapper;
    private final EnvioMapper envioMapper;
    private final CotizacionEnvioService cotizacionEnvioService;
    private final ICuponService cuponService;
    private final FacturaPdfGenerator facturaPdfGenerator;
    private final NotificacionClienteService notificacionClienteService;
    private final CarritoRepository carritoRepository;
    private final IReembolsoPagos reembolsoPagos;

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

        // La factura lleva el DNI. Las cuentas creadas con Google o GitHub no lo tienen hasta que lo cargan.
        if (cliente.getDni() == null || cliente.getDni().isBlank()) {
            throw new EcommerceException("Completá tu DNI en Mi perfil antes de confirmar la compra.");
        }

        List<Producto> productos = new ArrayList<>();
        List<Variante> variantes = new ArrayList<>();

        for (ItemPedidoRequest itemReq : request.items()) {
            Producto producto = productoRepository.findById(itemReq.idProducto())
                    .orElseThrow(() -> EntidadNoEncontradaException.producto(itemReq.idProducto()));
            if (!producto.isActivo()) {
                throw new EcommerceException("El producto '" + producto.getNombre() + "' ya no está a la venta.");
            }
            Variante variante = null;
            if (itemReq.idVariante() != null) {
                variante = varianteRepository.findById(itemReq.idVariante())
                        .orElseThrow(() -> EntidadNoEncontradaException.variante(itemReq.idVariante()));
                // Cada variante tiene su precio y su stock: no se puede combinar la variante de un
                // producto con otro producto (por ejemplo, para llevarse su oferta).
                if (!variante.getProducto().getId().equals(producto.getId())) {
                    throw new EcommerceException("La variante " + variante.getId()
                            + " no pertenece al producto " + producto.getId() + ".");
                }
                if (!variante.isActivo()) {
                    throw new EcommerceException("La variante " + variante.getId() + " ya no está a la venta.");
                }
                if (variante.getStock() < itemReq.cantidad()) {
                    throw StockInsuficienteException.stockInsuficiente(
                            itemReq.idVariante(), variante.getStock(), itemReq.cantidad());
                }
            } else if (!varianteRepository.findByProductoIdAndActivoTrue(producto.getId()).isEmpty()) {
                // Sin variante se cobraría el precio base y no se controlaría ni descontaría stock.
                throw new EcommerceException("El producto '" + producto.getNombre()
                        + "' tiene variantes: indicá cuál querés (idVariante).");
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
                .costoEnvio(cotizarEnvio(direccionEnvio, request))
                .build();

        for (int i = 0; i < request.items().size(); i++) {
            ItemPedidoRequest itemReq = request.items().get(i);
            ItemPedido item = ItemPedido.builder()
                    .pedido(pedido)
                    .producto(productos.get(i))
                    .variante(variantes.get(i))
                    .cantidad(itemReq.cantidad())
                    .precioProducto(productos.get(i).precioUnitario(variantes.get(i)) * itemReq.cantidad())
                    .build();
            pedido.getItems().add(item);
        }

        aplicarCupon(pedido, request.codigoCupon());

        return pedidoMapper.toResponse(pedidoRepository.save(pedido));
    }

    /**
     * Valida el cupón contra el subtotal de productos (sin envío), le suma un uso y deja el
     * descuento congelado en el pedido. Si el cupón no sirve, el pedido no se crea y el
     * cliente ve el motivo (vencido, agotado, compra mínima).
     */
    private void aplicarCupon(Pedido pedido, String codigoCupon) {
        if (codigoCupon == null || codigoCupon.isBlank()) {
            return;
        }
        ICuponService.CuponAplicado aplicado = cuponService.aplicar(codigoCupon, subtotalProductos(pedido));
        pedido.setCupon(aplicado.cupon());
        pedido.setDescuentoCupon(aplicado.descuento());
    }

    /**
     * El costo de envío lo cotiza el back para la dirección y la opción elegida:
     * nunca se acepta un precio mandado por el front. Sin dirección no hay envío.
     */
    private double cotizarEnvio(Direccion direccionEnvio, PedidoRequest request) {
        if (direccionEnvio == null) {
            return 0.0;
        }
        if (request.opcionEnvio() == null) {
            throw new EcommerceException("Tenés que elegir una opción de envío.");
        }
        return cotizacionEnvioService.precioDeOpcion(direccionEnvio.getId(), request.opcionEnvio());
    }

    /**
     * RF04: el cliente cancela su pedido mientras no esté despachado (pagado o no); el admin,
     * también uno despachado. Si ya estaba cobrado, primero se devuelve la plata en la pasarela:
     * si eso falla, el pedido no se cancela.
     */
    public CancelacionPedidoResponse cancelar(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> EntidadNoEncontradaException.pedido(id));
        verificarPropietario(pedido);

        EstadoPedido estado = pedido.getEstadoPedido();

        if (estado == EstadoPedido.ENTREGADO || estado == EstadoPedido.CANCELADO) {
            throw PedidoNoModificableException.noCancelable(id, estado);
        }

        if (getPrincipal().getRol() == Rol.CLIENTE && !clientePuedeCancelar(estado)) {
            throw PedidoNoModificableException.clienteNoPuedeCancelar(id, estado);
        }

        boolean estabaCobrado = estado == EstadoPedido.PAGADO || estado == EstadoPedido.DESPACHADO;

        // La devolución va antes que todo lo demás: si la pasarela la rechaza, no se tocó nada.
        TipoReembolso reembolso = TipoReembolso.NO_CORRESPONDE;
        double montoReembolsado = 0;
        if (estabaCobrado) {
            montoReembolsado = reembolsoPagos.reembolsarCobros(pedido);
            reembolso = montoReembolsado > 0 ? TipoReembolso.AUTOMATICO : TipoReembolso.MANUAL;
            if (reembolso == TipoReembolso.MANUAL) {
                // Lo marcó pagado el admin, sin pasarela: la tienda tiene que devolverlo por su cuenta.
                montoReembolsado = calcularTotal(pedido);
                log.warn("Pedido {} cancelado ya cobrado sin pasarela: hay que devolver ${} a mano.", id, montoReembolsado);
            }
        }

        if (estabaCobrado) {
            for (ItemPedido item : pedido.getItems()) {
                Variante variante = item.getVariante();
                if (variante != null) {
                    variante.setStock(variante.getStock() + item.getCantidad());
                    varianteRepository.save(variante);
                }
            }
        }

        // El uso del cupón vuelve a estar disponible.
        if (pedido.getCupon() != null) {
            cuponService.devolverUso(pedido.getCupon());
        }

        pedido.setEstadoPedido(EstadoPedido.CANCELADO);
        pedidoRepository.save(pedido);

        // Si lo canceló el propio cliente no hace falta avisarle.
        if (getPrincipal().getRol() == Rol.ADMIN) {
            notificacionClienteService.avisarPedido(pedido);
        }

        return new CancelacionPedidoResponse(id, EstadoPedido.CANCELADO, reembolso, montoReembolsado,
                mensajeCancelacion(id, reembolso, montoReembolsado));
    }

    /** RF04: el cliente cancela mientras el pedido no salió (pagado o no). Despachado, ya no. */
    static boolean clientePuedeCancelar(EstadoPedido estado) {
        return estado == EstadoPedido.PENDIENTE_PAGO
                || estado == EstadoPedido.EN_PREPARACION
                || estado == EstadoPedido.PAGADO;
    }

    private static String mensajeCancelacion(Long id, TipoReembolso reembolso, double monto) {
        String importe = FORMATO_PESOS.format(monto);
        return switch (reembolso) {
            case AUTOMATICO -> "Pedido #" + id + " cancelado. Se devolvieron " + importe
                    + " en Mercado Pago; puede tardar unos días en verse en el medio de pago.";
            case MANUAL -> "Pedido #" + id + " cancelado. El pago se registró fuera de Mercado Pago: "
                    + "la tienda tiene que devolver " + importe + " por su cuenta.";
            case NO_CORRESPONDE -> "Pedido #" + id + " cancelado. No se había cobrado nada.";
        };
    }

    private static final java.text.NumberFormat FORMATO_PESOS =
            java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("es-AR"));

    // ── Lo que necesita PagoService (pasarelas de pago) ──────────────────────
    // El pago con pasarelas vive en el paquete pago/. Acá queda solo lo que es del pedido
    // (Experto en Información): quién puede pagarlo y qué pasa cuando se cobra.

    /** El pedido del cliente logueado, listo para iniciar un pago. */
    @Transactional(readOnly = true)
    public Pedido obtenerParaPagar(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> EntidadNoEncontradaException.pedido(id));
        verificarPropietario(pedido);
        if (pedido.getEstadoPedido() != EstadoPedido.PENDIENTE_PAGO) {
            throw PedidoNoModificableException.porEstado(id, pedido.getEstadoPedido());
        }
        // Inicializa los ítems y sus productos para armar el cobro fuera de esta transacción.
        pedido.getItems().forEach(item -> item.getProducto().getNombre());
        return pedido;
    }

    /** Falla con 403 si el pedido no es del cliente logueado (un admin puede ver todos). */
    public void verificarAcceso(Pedido pedido) {
        verificarPropietario(pedido);
    }

    /**
     * Una pasarela confirmó el cobro: pasa el pedido a PAGADO (descuenta stock, emite factura
     * y avisa por mail). Bloquea la fila del pedido: el webhook y la página de vuelta pueden
     * llegar a la vez y solo el primero tiene que cobrar.
     *
     * @return true si el pedido quedó pagado (ahora o antes); false si el monto no alcanza
     *         o el pedido ya no se puede cobrar (por ejemplo, cancelado)
     */
    public boolean registrarPagoAprobado(Long pedidoId, TipoFactura tipoFactura, BigDecimal montoPagado) {
        Pedido pedido = pedidoRepository.findByIdParaActualizar(pedidoId)
                .orElseThrow(() -> EntidadNoEncontradaException.pedido(pedidoId));

        if (!esCobrable(pedido.getEstadoPedido())) {
            return pedido.getEstadoPedido() != EstadoPedido.CANCELADO;
        }

        BigDecimal total = BigDecimal.valueOf(calcularTotal(pedido));
        if (montoPagado == null || montoPagado.add(TOLERANCIA_REDONDEO).compareTo(total) < 0) {
            log.warn("Pago del pedido {} por ${} no cubre el total ${}: no se acredita.",
                    pedidoId, montoPagado, total);
            return false;
        }

        acreditarPago(pedido, tipoFactura);
        return true;
    }

    /** Margen por redondeo de centavos en la pasarela. */
    private static final BigDecimal TOLERANCIA_REDONDEO = new BigDecimal("0.05");

    /** Productos menos el cupón, más envío: es lo que se cobra en la pasarela y lo que va a la factura. */
    public double calcularTotal(Pedido pedido) {
        return subtotalProductos(pedido) - descuentoCupon(pedido) + costoEnvio(pedido);
    }

    private static double subtotalProductos(Pedido pedido) {
        return pedido.getItems().stream().mapToDouble(ItemPedido::getPrecioProducto).sum();
    }

    private static double descuentoCupon(Pedido pedido) {
        return pedido.getDescuentoCupon() != null ? pedido.getDescuentoCupon() : 0.0;
    }

    private static double costoEnvio(Pedido pedido) {
        return pedido.getCostoEnvio() != null ? pedido.getCostoEnvio() : 0.0;
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
                // Bloquea la fila: dos pagos simultáneos de la última unidad no pueden venderla dos veces.
                variante = varianteRepository.findByIdParaActualizar(variante.getId()).orElse(variante);
                if (variante.getStock() < item.getCantidad()) {
                    throw StockInsuficienteException.stockInsuficiente(
                            variante.getId(), variante.getStock(), item.getCantidad());
                }
                variante.setStock(variante.getStock() - item.getCantidad());
                varianteRepository.save(variante);
            }
        }

        double total = calcularTotal(pedido);

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
        notificacionClienteService.avisarPedido(guardado);
        quitarDelCarrito(pedido);

        return guardado;
    }

    /**
     * El carrito ya no se vacía al confirmar el checkout (el cliente puede arrepentirse y volver a
     * cambiarlo antes de pagar): lo comprado sale del carrito recién cuando se cobra. Se descuenta
     * solo lo de este pedido, por si después de confirmarlo agregó otras cosas.
     */
    private void quitarDelCarrito(Pedido pedido) {
        carritoRepository.findByClienteId(pedido.getCliente().getId()).ifPresent(carrito -> {
            for (ItemPedido comprado : pedido.getItems()) {
                Long idProducto = comprado.getProducto() == null ? null : comprado.getProducto().getId();
                Long idVariante = comprado.getVariante() == null ? null : comprado.getVariante().getId();
                carrito.getItems().stream()
                        .filter(item -> item.getProducto().getId().equals(idProducto)
                                && Objects.equals(
                                        item.getVariante() == null ? null : item.getVariante().getId(), idVariante))
                        .findFirst()
                        .ifPresent(item -> item.setCantidad(item.getCantidad() - comprado.getCantidad()));
            }
            carrito.getItems().removeIf(item -> item.getCantidad() <= 0);
            carritoRepository.save(carrito);
        });
    }

    /**
     * Cancela los pedidos pendientes del cliente que nunca llegaron a la pasarela: quedan cuando confirma
     * el checkout y vuelve al carrito sin ir a pagar. Los que tienen un intento de pago no se tocan, porque
     * el pago todavía puede acreditarse. Lo llama el checkout antes de crear el pedido nuevo.
     */
    public void cancelarPendientesSinIntentoDePago(Long idCliente) {
        for (Pedido abandonado : pedidoRepository.findPendientesSinIntentoDePago(idCliente)) {
            if (abandonado.getCupon() != null) {
                cuponService.devolverUso(abandonado.getCupon());
            }
            abandonado.setEstadoPedido(EstadoPedido.CANCELADO);
            pedidoRepository.save(abandonado);
            log.info("Pedido {} cancelado: el cliente {} volvió a confirmar sin haberlo pagado.",
                    abandonado.getId(), idCliente);
        }
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

        if (nuevo == EstadoPedido.ENTREGADO) {
            pedido.marcarEntregado();
        } else {
            pedido.setEstadoPedido(nuevo);
        }
        Pedido guardado = pedidoRepository.save(pedido);
        notificacionClienteService.avisarPedido(guardado);
        return pedidoMapper.toResponse(guardado);
    }

    /**
     * RF03: el cliente avisa si le llegó el pedido que figura entregado. "No lo recibí" queda
     * registrado (con el comentario) para que lo revise la tienda, y después todavía puede pasar a
     * "Lo recibí" si el paquete aparece. "Lo recibí" es definitivo.
     */
    public PedidoResponse confirmarRecepcion(Long id, ConfirmarRecepcionRequest request) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> EntidadNoEncontradaException.pedido(id));

        UsuarioDetails principal = getPrincipal();
        if (principal.getRol() != Rol.CLIENTE || !principal.getId().equals(pedido.getCliente().getId())) {
            throw new AccessDeniedException("Solo el cliente que hizo el pedido puede confirmar si lo recibió.");
        }

        if (pedido.getEstadoPedido() != EstadoPedido.ENTREGADO) {
            throw new PedidoNoModificableException(
                    "Solo podés confirmar la recepción de un pedido entregado. El pedido #" + id
                    + " está " + pedido.getEstadoPedido() + ".");
        }

        ConfirmacionRecepcion actual = pedido.getConfirmacionRecepcion();
        boolean recibido = request.recibido();
        if (actual == ConfirmacionRecepcion.RECIBIDO) {
            throw new ConflictoException("Ya confirmaste que recibiste el pedido #" + id + ".");
        }
        if (actual == ConfirmacionRecepcion.NO_RECIBIDO && !recibido) {
            throw new ConflictoException("Ya nos avisaste que el pedido #" + id
                    + " no te llegó. Lo estamos revisando.");
        }

        pedido.setConfirmacionRecepcion(recibido ? ConfirmacionRecepcion.RECIBIDO : ConfirmacionRecepcion.NO_RECIBIDO);
        pedido.setFechaConfirmacionRecepcion(LocalDateTime.now());
        if (recibido) {
            pedido.setComentarioRecepcion(null);
        } else {
            String comentario = request.comentario() == null ? "" : request.comentario().trim();
            pedido.setComentarioRecepcion(comentario.isEmpty() ? null : comentario);
            log.warn("El cliente {} avisó que no recibió el pedido {} (figura ENTREGADO).",
                    principal.getId(), id);
        }
        return pedidoMapper.toResponse(pedidoRepository.save(pedido));
    }

    /**
     * RF05: el cliente pide el reembolso de su pedido entregado, dentro de los 10 días de la
     * entrega. Queda SOLICITADO hasta que lo resuelva el admin. Se puede pedir una sola vez.
     */
    public PedidoResponse solicitarReembolso(Long id, SolicitudReembolsoRequest request) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> EntidadNoEncontradaException.pedido(id));

        UsuarioDetails principal = getPrincipal();
        if (principal.getRol() != Rol.CLIENTE || !principal.getId().equals(pedido.getCliente().getId())) {
            throw new AccessDeniedException("Solo el cliente que hizo el pedido puede pedir el reembolso.");
        }

        if (pedido.getEstadoPedido() != EstadoPedido.ENTREGADO) {
            throw new PedidoNoModificableException(
                    "Solo podés pedir el reembolso de un pedido entregado. El pedido #" + id
                    + " está " + pedido.getEstadoPedido() + ".");
        }
        if (pedido.getEstadoReembolso() != null) {
            throw new ConflictoException(switch (pedido.getEstadoReembolso()) {
                case SOLICITADO -> "Ya pediste el reembolso del pedido #" + id + ". Lo estamos revisando.";
                case APROBADO -> "El reembolso del pedido #" + id + " ya fue aprobado.";
                case RECHAZADO -> "El reembolso del pedido #" + id + " ya fue rechazado. "
                        + "Si no estás de acuerdo, comunicate con la tienda.";
            });
        }
        LocalDateTime limite = pedido.getFechaLimiteReembolso();
        if (limite == null || LocalDateTime.now().isAfter(limite)) {
            throw new PedidoNoModificableException(
                    "Pasaron más de " + Pedido.PLAZO_REEMBOLSO_DIAS + " días desde la entrega del pedido #" + id
                    + ": ya no se puede pedir el reembolso desde la página. Comunicate con la tienda.");
        }

        pedido.setEstadoReembolso(EstadoReembolso.SOLICITADO);
        pedido.setMotivoReembolso(request.motivo().trim());
        pedido.setFechaSolicitudReembolso(LocalDateTime.now());
        log.info("El cliente {} pidió el reembolso del pedido {}.", principal.getId(), id);
        return pedidoMapper.toResponse(pedidoRepository.save(pedido));
    }

    /**
     * RF05: el admin resuelve el reembolso pedido. Si lo aprueba, se devuelve la plata en Mercado
     * Pago antes de marcarlo (si falla, queda SOLICITADO). El stock no vuelve: el producto ya se
     * entregó. Al rechazarlo tiene que decir por qué. En los dos casos se avisa al cliente.
     */
    public PedidoResponse resolverReembolso(Long id, ResolucionReembolsoRequest request) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> EntidadNoEncontradaException.pedido(id));

        if (pedido.getEstadoReembolso() != EstadoReembolso.SOLICITADO) {
            throw new ConflictoException(pedido.getEstadoReembolso() == null
                    ? "El cliente no pidió el reembolso del pedido #" + id + "."
                    : "El reembolso del pedido #" + id + " ya fue resuelto ("
                            + pedido.getEstadoReembolso().name().toLowerCase() + ").");
        }

        String respuesta = request.respuesta() == null ? "" : request.respuesta().trim();
        if (Boolean.TRUE.equals(request.aprobar())) {
            double devuelto = reembolsoPagos.reembolsarCobros(pedido);
            if (devuelto > 0) {
                pedido.setTipoReembolso(TipoReembolso.AUTOMATICO);
                pedido.setMontoReembolsado(devuelto);
            } else {
                // Se pagó sin pasarela (lo marcó pagado el admin): la tienda lo devuelve por su cuenta.
                pedido.setTipoReembolso(TipoReembolso.MANUAL);
                pedido.setMontoReembolsado(calcularTotal(pedido));
            }
            pedido.setEstadoReembolso(EstadoReembolso.APROBADO);
        } else {
            if (respuesta.isEmpty()) {
                throw new EcommerceException("Para rechazar el reembolso, contale al cliente por qué.");
            }
            pedido.setEstadoReembolso(EstadoReembolso.RECHAZADO);
        }
        pedido.setRespuestaReembolso(respuesta.isEmpty() ? null : respuesta);
        pedido.setFechaResolucionReembolso(LocalDateTime.now());

        Pedido guardado = pedidoRepository.save(pedido);
        notificacionClienteService.avisarReembolso(guardado);
        return pedidoMapper.toResponse(guardado);
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
    public FacturaPdfResponse obtenerFacturaPdf(Long idPedido) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> EntidadNoEncontradaException.pedido(idPedido));
        verificarPropietario(pedido);
        Factura factura = pedido.getFactura();
        if (factura == null) {
            throw new ConflictoException("El pedido con id " + idPedido + " aún no tiene factura.");
        }
        return new FacturaPdfResponse(facturaPdfGenerator.nombreArchivo(factura), facturaPdfGenerator.generar(factura));
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
                .empresa(request.empresa().trim())
                .numeroSeguimiento(request.numeroSeguimiento().trim())
                .build();
        envioRepository.save(envio);

        pedido.setEstadoPedido(EstadoPedido.DESPACHADO);
        pedido.setEnvio(envio);
        pedidoRepository.save(pedido);

        notificacionService.notificarEnvioActualizado(envio);
        notificacionClienteService.avisarEnvio(envio);

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
            pedido.marcarEntregado();
            pedidoRepository.save(pedido);
        }

        envioRepository.save(envio);
        notificacionService.notificarEnvioActualizado(envio);
        notificacionClienteService.avisarEnvio(envio);

        return envioMapper.toResponse(envio);
    }
}
