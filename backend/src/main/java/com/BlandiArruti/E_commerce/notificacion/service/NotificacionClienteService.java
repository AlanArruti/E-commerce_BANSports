package com.BlandiArruti.E_commerce.notificacion.service;

import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.enums.EstadoEnvio;
import com.BlandiArruti.E_commerce.enums.Rol;
import com.BlandiArruti.E_commerce.enums.TipoNotificacion;
import com.BlandiArruti.E_commerce.envio.entity.Envio;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.notificacion.dto.response.NoLeidasResponse;
import com.BlandiArruti.E_commerce.notificacion.dto.response.NotificacionResponse;
import com.BlandiArruti.E_commerce.notificacion.entity.Notificacion;
import com.BlandiArruti.E_commerce.notificacion.repository.NotificacionRepository;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Notificaciones que el cliente ve en la página (RF02): cambios de estado de sus pedidos y
 * productos que entran en oferta. Los mails son otra cosa y los manda {@link NotificacionService}.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificacionClienteService {

    /** La campanita muestra las últimas; las más viejas quedan guardadas pero no se listan. */
    static final int MAXIMO_LISTADO = 30;

    private final NotificacionRepository notificacionRepository;

    // ── Lo que ve el cliente ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificacionResponse> listar(Long idCliente) {
        verificarPropietario(idCliente);
        return notificacionRepository
                .findByClienteIdOrderByFechaCreacionDescIdDesc(idCliente, PageRequest.of(0, MAXIMO_LISTADO))
                .stream()
                .map(NotificacionClienteService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NoLeidasResponse contarNoLeidas(Long idCliente) {
        verificarPropietario(idCliente);
        return new NoLeidasResponse(notificacionRepository.countByClienteIdAndLeidaFalse(idCliente));
    }

    public NotificacionResponse marcarLeida(Long idCliente, Long idNotificacion) {
        verificarPropietario(idCliente);
        Notificacion notificacion = notificacionRepository.findByIdAndClienteId(idNotificacion, idCliente)
                .orElseThrow(() -> new EntidadNoEncontradaException(
                        "Notificación con id " + idNotificacion + " no encontrada."));
        notificacion.setLeida(true);
        return toResponse(notificacion);
    }

    public NoLeidasResponse marcarTodasLeidas(Long idCliente) {
        verificarPropietario(idCliente);
        notificacionRepository.marcarTodasComoLeidas(idCliente);
        return new NoLeidasResponse(0);
    }

    // ── Avisos que generan los otros servicios ───────────────────────────────

    /**
     * Avisa al dueño del pedido el estado en que quedó. Se llama después de cambiarlo
     * (pago, preparación, despacho, entrega, cancelación).
     */
    public void avisarPedido(Pedido pedido) {
        avisar(pedido, pedido.getEnvio());
    }

    /** Igual que {@link #avisarPedido}, pero para cambios del envío (EN_CAMINO no cambia el pedido). */
    public void avisarEnvio(Envio envio) {
        avisar(envio.getPedido(), envio);
    }

    /** Un producto entró en oferta (o mejoró el descuento): les llega a todos los clientes activos. */
    public void avisarOferta(Producto producto) {
        String titulo = "¡Nueva oferta! " + producto.getDescuento() + "% OFF";
        String mensaje = recortar(producto.getNombre() + " ahora tiene " + producto.getDescuento()
                + "% de descuento. Aprovechalo mientras dure.", 300);
        int cantidad = notificacionRepository.crearParaClientesActivos(
                TipoNotificacion.OFERTA.name(), titulo, mensaje, producto.getId(), LocalDateTime.now());
        log.info("Oferta del producto {} avisada a {} clientes.", producto.getId(), cantidad);
    }

    private void avisar(Pedido pedido, Envio envio) {
        if (pedido == null || pedido.getCliente() == null) {
            return;
        }
        Long numero = pedido.getId();
        String titulo;
        String mensaje;
        switch (pedido.getEstadoPedido()) {
            case PAGADO -> {
                titulo = "Pago acreditado";
                mensaje = "Recibimos el pago del pedido #" + numero + ". Ya lo estamos preparando para el envío.";
            }
            case EN_PREPARACION -> {
                titulo = "Pedido en preparación";
                mensaje = "Estamos preparando tu pedido #" + numero + ".";
            }
            case DESPACHADO -> {
                if (envio != null && envio.getEstado() == EstadoEnvio.EN_CAMINO) {
                    titulo = "Pedido en camino";
                    mensaje = "Tu pedido #" + numero + " está en camino.";
                } else {
                    titulo = "Pedido despachado";
                    mensaje = "Tu pedido #" + numero + " fue despachado" + datosDelEnvio(envio) + ".";
                }
            }
            case ENTREGADO -> {
                titulo = "Pedido entregado";
                mensaje = "Tu pedido #" + numero + " fue entregado. ¡Gracias por tu compra!";
            }
            case CANCELADO -> {
                titulo = "Pedido cancelado";
                mensaje = "Tu pedido #" + numero + " fue cancelado.";
            }
            // PENDIENTE_PAGO es el estado inicial: el cliente acaba de crear el pedido, no hay nada que avisar.
            default -> {
                return;
            }
        }

        notificacionRepository.save(Notificacion.builder()
                .cliente(pedido.getCliente())
                .tipo(TipoNotificacion.PEDIDO)
                .titulo(titulo)
                .mensaje(recortar(mensaje, 300))
                .idReferencia(numero)
                .fechaCreacion(LocalDateTime.now())
                .build());
    }

    private static String datosDelEnvio(Envio envio) {
        if (envio == null || envio.getEmpresa() == null) {
            return "";
        }
        String datos = " con " + envio.getEmpresa();
        if (envio.getNumeroSeguimiento() != null && !envio.getNumeroSeguimiento().isBlank()) {
            datos += " (seguimiento " + envio.getNumeroSeguimiento() + ")";
        }
        return datos;
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 1) + "…";
    }

    private static NotificacionResponse toResponse(Notificacion n) {
        return new NotificacionResponse(n.getId(), n.getTipo(), n.getTitulo(), n.getMensaje(),
                n.getIdReferencia(), n.isLeida(), n.getFechaCreacion());
    }

    private void verificarPropietario(Long idCliente) {
        UsuarioDetails principal = (UsuarioDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        if (principal.getRol() == Rol.CLIENTE && !principal.getId().equals(idCliente)) {
            throw new AccessDeniedException("No tenés permiso para ver estas notificaciones.");
        }
    }
}
