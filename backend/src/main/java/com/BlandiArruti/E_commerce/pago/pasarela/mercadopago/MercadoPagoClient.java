package com.BlandiArruti.E_commerce.pago.pasarela.mercadopago;

import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.exception.ServicioExternoException;
import com.BlandiArruti.E_commerce.pago.pasarela.mercadopago.dto.OrdenMercadoPago;
import com.BlandiArruti.E_commerce.pago.pasarela.mercadopago.dto.OrdenRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API de Orders de MercadoPago (Checkout Pro). Se llama directo por HTTP: es la API que
 * MercadoPago recomienda para integraciones nuevas (la de Preferences quedó como legacy).
 *
 * Docs: https://www.mercadopago.com.ar/developers/es/docs/checkout-pro-orders/overview
 */
@Component
@Slf4j
public class MercadoPagoClient {

    private static final String BASE_URL = "https://api.mercadopago.com";

    private final MercadoPagoProperties properties;
    private final RestClient restClient;

    public MercadoPagoClient(MercadoPagoProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().baseUrl(BASE_URL).build();
    }

    public OrdenMercadoPago crearOrden(OrdenRequest orden) {
        try {
            return restClient.post()
                    .uri("/v1/orders")
                    .headers(h -> {
                        h.setBearerAuth(properties.getAccessToken());
                        // Evita crear dos órdenes si el mismo pedido se reintenta por un corte de red.
                        h.set("X-Idempotency-Key", UUID.randomUUID().toString());
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(orden)
                    .retrieve()
                    .body(OrdenMercadoPago.class);
        } catch (RestClientResponseException e) {
            log.error("MercadoPago rechazó la order ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw traducir("crear el cobro", e);
        } catch (RestClientException e) {
            throw new ServicioExternoException("No se pudo conectar con Mercado Pago para crear el cobro (sin respuesta).", e);
        }
    }

    public OrdenMercadoPago obtenerOrden(String idOrden) {
        try {
            return restClient.get()
                    .uri("/v1/orders/{id}", idOrden)
                    .headers(h -> h.setBearerAuth(properties.getAccessToken()))
                    .retrieve()
                    .body(OrdenMercadoPago.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                throw new EcommerceException("Mercado Pago no tiene registrado el cobro " + idOrden + ".");
            }
            log.error("Error consultando la order {} en MercadoPago ({}): {}", idOrden, e.getStatusCode(), e.getResponseBodyAsString());
            throw traducir("consultar el cobro", e);
        } catch (RestClientException e) {
            throw new ServicioExternoException("No se pudo conectar con Mercado Pago para consultar el cobro (sin respuesta).", e);
        }
    }

    /**
     * Error de la API de MercadoPago → mensaje puntual para el cliente. Se muestra el motivo que
     * da MercadoPago cuando rechaza los datos (no tiene información sensible); el token y los
     * detalles internos quedan solo en el log.
     */
    private RuntimeException traducir(String accion, RestClientResponseException e) {
        int codigo = e.getStatusCode().value();
        if (codigo == 401 || codigo == 403) {
            return new ServicioExternoException(
                    "No se pudo " + accion + ": las credenciales de Mercado Pago de la tienda no son válidas o no tienen permiso.", e);
        }
        if (codigo == 429) {
            return new ServicioExternoException(
                    "No se pudo " + accion + ": Mercado Pago está limitando las solicitudes. Probá de nuevo en un minuto.", e);
        }
        if (codigo >= 500) {
            return new ServicioExternoException(
                    "No se pudo " + accion + ": Mercado Pago no responde (error " + codigo + "). Probá de nuevo en unos minutos.", e);
        }
        String motivo = motivoDeMercadoPago(e.getResponseBodyAsString());
        return new ServicioExternoException("Mercado Pago rechazó " + (accion.startsWith("crear") ? "el cobro" : "la consulta")
                + (motivo.isEmpty() ? " (error " + codigo + ")." : ": " + motivo), e);
    }

    /** Saca el "message" del error que devuelve MercadoPago, por ejemplo "total_amount must be...". */
    private static String motivoDeMercadoPago(String cuerpo) {
        Matcher m = MENSAJE_MP.matcher(cuerpo == null ? "" : cuerpo);
        return m.find() ? m.group(1) : "";
    }

    private static final Pattern MENSAJE_MP = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]{1,200})\"");
}
