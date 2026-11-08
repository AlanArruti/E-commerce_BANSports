package com.BlandiArruti.E_commerce.pago.service;

import com.BlandiArruti.E_commerce.enums.EstadoPago;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.pago.dto.request.IniciarPagoRequest;
import com.BlandiArruti.E_commerce.pago.dto.response.InicioPagoResponse;
import com.BlandiArruti.E_commerce.pago.dto.response.PagoResponse;
import com.BlandiArruti.E_commerce.pago.dto.response.PasarelaResponse;
import com.BlandiArruti.E_commerce.pago.entity.Pago;
import com.BlandiArruti.E_commerce.pago.pasarela.PasarelaPago;
import com.BlandiArruti.E_commerce.pago.pasarela.RegistroPasarelas;
import com.BlandiArruti.E_commerce.pago.pasarela.dto.CobroIniciado;
import com.BlandiArruti.E_commerce.pago.pasarela.dto.ResultadoCobro;
import com.BlandiArruti.E_commerce.pago.pasarela.dto.SolicitudCobro;
import com.BlandiArruti.E_commerce.pago.repository.PagoRepository;
import com.BlandiArruti.E_commerce.pedido.entity.ItemPedido;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import com.BlandiArruti.E_commerce.pedido.service.IPedidoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Caso de uso "pagar un pedido" (Controller de GRASP). Coordina tres cosas sin saber qué
 * pasarela concreta se usa:
 * <ol>
 *   <li>{@link #iniciar}: registra el intento ({@link Pago}) y le pide el cobro a la pasarela.</li>
 *   <li>{@link #confirmar}: al volver el cliente, consulta el estado real en la pasarela.</li>
 *   <li>{@link #procesarNotificacion}: lo mismo, cuando la pasarela avisa por webhook.</li>
 * </ol>
 * Lo que le pasa al pedido cuando se cobra lo decide el pedido (IPedidoService#registrarPagoAprobado).
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PagoService {

    private final RegistroPasarelas registroPasarelas;
    private final PagoRepository pagoRepository;
    private final IPedidoService pedidoService;

    @Value("${app.front-url:http://localhost:4200}")
    private String frontUrl;

    @Transactional(readOnly = true)
    public List<PasarelaResponse> pasarelasDisponibles() {
        return registroPasarelas.disponibles().stream()
                .map(p -> new PasarelaResponse(p.codigo(), p.nombre()))
                .toList();
    }

    public InicioPagoResponse iniciar(Long pedidoId, IniciarPagoRequest request) {
        PasarelaPago pasarela = registroPasarelas.obtener(request.pasarela());
        Pedido pedido = pedidoService.obtenerParaPagar(pedidoId);

        Pago pago = pagoRepository.save(Pago.builder()
                .pedido(pedido)
                .pasarela(pasarela.codigo())
                .estado(EstadoPago.CREADO)
                .tipoFactura(request.tipoFactura())
                .monto(pedidoService.calcularTotal(pedido))
                .build());

        CobroIniciado cobro = pasarela.iniciarCobro(new SolicitudCobro(
                pago.getId().toString(),
                "Pedido #" + pedido.getId() + " - BAN Sports",
                lineas(pedido),
                urlVuelta(pago)));

        pago.setIdExterno(cobro.idExterno());
        return new InicioPagoResponse(pago.getId(), cobro.urlPago());
    }

    /** Lo llama la pantalla de vuelta del front. Así el pago se confirma aunque el webhook no llegue. */
    public PagoResponse confirmar(Long pagoId) {
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Pago con id " + pagoId + " no encontrado."));
        pedidoService.verificarAcceso(pago.getPedido());
        actualizar(pago);
        return aResponse(pago);
    }

    /** Aviso de una pasarela al webhook. Nunca lanza: la pasarela reintenta si el webhook falla. */
    public void procesarNotificacion(String codigoPasarela, Map<String, Object> cuerpo, Map<String, String> parametros) {
        PasarelaPago pasarela = registroPasarelas.obtener(codigoPasarela);
        pasarela.leerNotificacion(cuerpo, parametros).ifPresent(idExterno ->
                pagoRepository.findByPasarelaAndIdExterno(pasarela.codigo(), idExterno).ifPresentOrElse(
                        this::actualizar,
                        () -> log.info("Aviso de {} por un cobro que no es nuestro: {}", pasarela.codigo(), idExterno)));
    }

    @Transactional(readOnly = true)
    public List<PagoResponse> historial(Long pedidoId) {
        pedidoService.buscarPorId(pedidoId); // verifica que el pedido sea del cliente logueado
        return pagoRepository.findByPedidoIdOrderByFechaCreacionDesc(pedidoId).stream()
                .map(this::aResponse)
                .toList();
    }

    /**
     * Consulta el cobro en su pasarela y aplica el resultado. Es idempotente: se puede llamar
     * muchas veces (webhooks repetidos, el cliente recargando la página) sin cobrar dos veces.
     */
    private void actualizar(Pago pago) {
        if (pago.getEstado() == EstadoPago.APROBADO || pago.getIdExterno() == null) {
            return;
        }

        ResultadoCobro resultado = registroPasarelas.obtener(pago.getPasarela()).consultarCobro(pago.getIdExterno());

        // La referencia la pusimos nosotros al crear el cobro: si no coincide, es un cobro de otro pago.
        if (!pago.getId().toString().equals(resultado.referencia())) {
            throw new EcommerceException("El cobro de " + pago.getPasarela() + " no corresponde a este pago.");
        }

        if (resultado.montoPagado() != null && resultado.montoPagado().signum() > 0) {
            pago.setMontoPagado(resultado.montoPagado().doubleValue());
        }

        if (resultado.estado() != EstadoPago.APROBADO) {
            pago.setEstado(resultado.estado());
            return;
        }

        boolean acreditado = pedidoService.registrarPagoAprobado(
                pago.getPedido().getId(), pago.getTipoFactura(), resultado.montoPagado());
        if (acreditado) {
            pago.setEstado(EstadoPago.APROBADO);
            log.info("Pago {} ({}) aprobado: pedido {} pagado.", pago.getId(), pago.getPasarela(), pago.getPedido().getId());
        } else {
            // Cobrado en la pasarela pero no aplicable (monto menor o pedido cancelado): hay que revisarlo a mano.
            pago.setEstado(EstadoPago.RECHAZADO);
            log.error("Pago {} cobrado en {} pero no acreditado en el pedido {}: requiere revisión.",
                    pago.getId(), pago.getPasarela(), pago.getPedido().getId());
        }
    }

    private List<SolicitudCobro.LineaCobro> lineas(Pedido pedido) {
        List<SolicitudCobro.LineaCobro> lineas = new ArrayList<>(pedido.getItems().stream().map(this::linea).toList());
        // El envío va como una línea más: la pasarela cobra la suma de las líneas y tiene que dar el total del pedido.
        double envio = pedido.getCostoEnvio() != null ? pedido.getCostoEnvio() : 0.0;
        if (envio > 0) {
            lineas.add(new SolicitudCobro.LineaCobro("ENVIO", "Envío a domicilio", 1, BigDecimal.valueOf(envio)));
        }
        return lineas;
    }

    private SolicitudCobro.LineaCobro linea(ItemPedido item) {
        String codigo = item.getVariante() != null
                ? "V" + item.getVariante().getId()
                : "P" + item.getProducto().getId();
        // El precio de la línea quedó congelado en el pedido: si después cambia una oferta, se cobra lo pactado.
        return new SolicitudCobro.LineaCobro(codigo, item.getProducto().getNombre(), item.getCantidad(),
                BigDecimal.valueOf(item.getPrecioProducto()));
    }

    private String urlVuelta(Pago pago) {
        String base = frontUrl.endsWith("/") ? frontUrl.substring(0, frontUrl.length() - 1) : frontUrl;
        return base + "/pago/resultado?pago=" + pago.getId();
    }

    private PagoResponse aResponse(Pago pago) {
        return new PagoResponse(pago.getId(), pago.getPedido().getId(), pago.getPasarela(), pago.getEstado(),
                pago.getTipoFactura(), pago.getMonto(), pago.getMontoPagado(),
                pago.getPedido().getEstadoPedido(), pago.getFechaCreacion());
    }
}
