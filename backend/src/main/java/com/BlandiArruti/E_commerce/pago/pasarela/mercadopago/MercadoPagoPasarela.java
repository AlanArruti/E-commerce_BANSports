package com.BlandiArruti.E_commerce.pago.pasarela.mercadopago;

import com.BlandiArruti.E_commerce.enums.EstadoPago;
import com.BlandiArruti.E_commerce.pago.pasarela.PasarelaPagoBase;
import com.BlandiArruti.E_commerce.pago.pasarela.dto.CobroIniciado;
import com.BlandiArruti.E_commerce.pago.pasarela.dto.ResultadoCobro;
import com.BlandiArruti.E_commerce.pago.pasarela.dto.SolicitudCobro;
import com.BlandiArruti.E_commerce.pago.pasarela.mercadopago.dto.OrdenMercadoPago;
import com.BlandiArruti.E_commerce.pago.pasarela.mercadopago.dto.OrdenRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MercadoPago Checkout Pro vía API de Orders (SCRUM-122). Hija de {@link PasarelaPagoBase}:
 * adapta la API de MercadoPago (orders, estados processed/accredited, tópico "order") al
 * idioma común de las pasarelas (Adapter).
 *
 * Docs: https://www.mercadopago.com.ar/developers/es/docs/checkout-pro-orders/overview
 */
@Component
public class MercadoPagoPasarela extends PasarelaPagoBase {

    public static final String CODIGO = "mercadopago";

    private final MercadoPagoProperties properties;
    private final MercadoPagoClient client;

    public MercadoPagoPasarela(MercadoPagoProperties properties, MercadoPagoClient client) {
        this.properties = properties;
        this.client = client;
    }

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public String nombre() {
        return "Mercado Pago";
    }

    @Override
    public boolean estaDisponible() {
        return properties.estaConfigurado();
    }

    @Override
    protected CobroIniciado crearCobro(SolicitudCobro solicitud) {
        // Una línea por producto con cantidad 1 y el total de la línea: la API exige que la suma
        // de los ítems dé exacto total_amount, y así no hay diferencias por redondear cada unidad.
        List<OrdenRequest.Item> items = solicitud.lineas().stream()
                .map(linea -> new OrdenRequest.Item(
                        linea.titulo() + (linea.cantidad() > 1 ? " (x" + linea.cantidad() + ")" : ""),
                        1,
                        formatearMonto(linea.monto()),
                        linea.codigo()))
                .toList();

        String vuelta = solicitud.urlVuelta();
        // MercadoPago solo vuelve solo al sitio si la URL es https (en un deploy).
        String autoReturn = vuelta.startsWith("https://") ? "approved" : null;

        OrdenRequest orden = new OrdenRequest(
                "online",
                "manual",       // único valor válido para Checkout Pro
                "automatic",    // resultado aprobado/rechazado apenas termina el checkout
                formatearMonto(solicitud.total()),
                solicitud.referencia(),
                solicitud.descripcion(),
                items,
                new OrdenRequest.Config(new OrdenRequest.Online(vuelta, vuelta, vuelta, autoReturn)));

        OrdenMercadoPago creada = client.crearOrden(orden);
        return creada == null ? null : new CobroIniciado(creada.id(), creada.checkoutUrl());
    }

    @Override
    protected ResultadoCobro buscarCobro(String idExterno) {
        OrdenMercadoPago orden = client.obtenerOrden(idExterno);
        if (orden == null) {
            return null;
        }
        return new ResultadoCobro(orden.id(), traducirEstado(orden), orden.montoPagado(), orden.externalReference());
    }

    /**
     * Aviso del tópico "order": {@code ?type=order&data.id=ORD...} y el mismo dato en el body.
     * Los avisos de otros tópicos se ignoran.
     */
    @Override
    protected Optional<String> extraerIdDeNotificacion(Map<String, Object> cuerpo, Map<String, String> parametros) {
        Object tipo = cuerpo.getOrDefault("type", parametros.get("type"));
        if (!"order".equals(tipo)) {
            return Optional.empty();
        }
        if (cuerpo.get("data") instanceof Map<?, ?> data && data.get("id") != null) {
            return Optional.of(data.get("id").toString());
        }
        return Optional.ofNullable(parametros.get("data.id"));
    }

    /** Estados de una order de MercadoPago → EstadoPago. */
    static EstadoPago traducirEstado(OrdenMercadoPago orden) {
        if (orden.estaCobrada()) {
            return EstadoPago.APROBADO;
        }
        String estado = orden.status() == null ? "" : orden.status();
        return switch (estado) {
            case "created" -> EstadoPago.CREADO;
            case "failed", "expired", "canceled", "cancelled", "refunded", "charged_back" -> EstadoPago.RECHAZADO;
            // processing, action_required, processed sin acreditar, o un estado nuevo que no conocemos.
            default -> EstadoPago.PENDIENTE;
        };
    }
}
