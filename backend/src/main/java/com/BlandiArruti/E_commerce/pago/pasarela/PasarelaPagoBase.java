package com.BlandiArruti.E_commerce.pago.pasarela;

import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.exception.ServicioExternoException;
import com.BlandiArruti.E_commerce.pago.pasarela.dto.CobroIniciado;
import com.BlandiArruti.E_commerce.pago.pasarela.dto.ResultadoCobro;
import com.BlandiArruti.E_commerce.pago.pasarela.dto.SolicitudCobro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

/**
 * Clase padre de todas las pasarelas (Template Method).
 *
 * Los métodos públicos son {@code final} y fijan el algoritmo común: validar antes de llamar a
 * la pasarela, controlar lo que devuelve y que un aviso mal formado nunca rompa el webhook.
 * Cada hija completa solo los pasos que cambian entre pasarelas:
 * {@link #crearCobro}, {@link #buscarCobro} y {@link #extraerIdDeNotificacion}.
 */
public abstract class PasarelaPagoBase implements PasarelaPago {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public final CobroIniciado iniciarCobro(SolicitudCobro solicitud) {
        if (!estaDisponible()) {
            throw new ServicioExternoException(
                    "El pago con " + nombre() + " no está disponible: la tienda todavía no cargó sus credenciales.");
        }
        if (solicitud.lineas() == null || solicitud.lineas().isEmpty()
                || solicitud.total().compareTo(BigDecimal.ZERO) <= 0) {
            throw new EcommerceException("No hay nada para cobrar en este pedido.");
        }

        CobroIniciado cobro = crearCobro(solicitud);

        if (cobro == null || esVacio(cobro.idExterno()) || esVacio(cobro.urlPago())) {
            throw new ServicioExternoException(nombre() + " no devolvió la página de pago. Intentá de nuevo.");
        }
        log.info("Cobro creado en {}: id={}, referencia={}", codigo(), cobro.idExterno(), solicitud.referencia());
        return cobro;
    }

    @Override
    public final ResultadoCobro consultarCobro(String idExterno) {
        if (esVacio(idExterno)) {
            throw new EcommerceException("Falta el id del cobro.");
        }
        ResultadoCobro resultado = buscarCobro(idExterno);
        if (resultado == null || resultado.estado() == null) {
            throw new ServicioExternoException(nombre() + " devolvió el cobro " + idExterno + " sin estado.");
        }
        return resultado;
    }

    @Override
    public final Optional<String> leerNotificacion(Map<String, Object> cuerpo, Map<String, String> parametros) {
        try {
            return extraerIdDeNotificacion(cuerpo == null ? Map.of() : cuerpo,
                                           parametros == null ? Map.of() : parametros)
                    .filter(id -> !id.isBlank());
        } catch (RuntimeException e) {
            // La pasarela reintenta si el webhook falla: un aviso que no entendemos se descarta.
            log.warn("Aviso de {} que no se pudo leer: {}", codigo(), e.getMessage());
            return Optional.empty();
        }
    }

    // ── Pasos que completa cada pasarela ─────────────────────────────────────

    /** Crea el cobro en la API de la pasarela. */
    protected abstract CobroIniciado crearCobro(SolicitudCobro solicitud);

    /** Busca el cobro en la API de la pasarela y traduce su estado a EstadoPago. */
    protected abstract ResultadoCobro buscarCobro(String idExterno);

    /** Saca el id del cobro del aviso, en el formato propio de la pasarela. */
    protected abstract Optional<String> extraerIdDeNotificacion(Map<String, Object> cuerpo,
                                                                 Map<String, String> parametros);

    // ── Utilidades para las hijas ────────────────────────────────────────────

    /** Monto con dos decimales y punto, como lo piden casi todas las APIs ("1500.00"). */
    protected static String formatearMonto(BigDecimal monto) {
        return monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    protected static boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
