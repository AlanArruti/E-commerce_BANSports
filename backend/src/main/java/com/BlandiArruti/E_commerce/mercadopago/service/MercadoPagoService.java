package com.BlandiArruti.E_commerce.mercadopago.service;

import com.BlandiArruti.E_commerce.enums.TipoFactura;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.mercadopago.config.MercadoPagoProperties;
import com.mercadopago.exceptions.MPApiException;
import com.BlandiArruti.E_commerce.mercadopago.dto.response.PreferenciaResponse;
import com.BlandiArruti.E_commerce.pedido.entity.ItemPedido;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoService {

    private final MercadoPagoProperties mercadoPagoProperties;

    public PreferenciaResponse crearPreferencia(Pedido pedido, TipoFactura tipoFactura) {
        try {
            List<PreferenceItemRequest> items = pedido.getItems().stream()
                    .map(this::mapItem)
                    .toList();

            String externalReference = pedido.getId() + "|" + tipoFactura.name();

            String base = mercadoPagoProperties.getBackUrl();
            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(base + "/pago/exitoso")
                    .failure(base + "/pago/fallido")
                    .pending(base + "/pago/pendiente")
                    .build();

            PreferenceRequest.PreferenceRequestBuilder builder = PreferenceRequest.builder()
                    .items(items)
                    .backUrls(backUrls)
                    .externalReference(externalReference);

            String notificationUrl = mercadoPagoProperties.getNotificationUrl();
            if (notificationUrl != null && !notificationUrl.isBlank()) {
                builder.notificationUrl(notificationUrl + "?source_news=webhooks");
            }

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(builder.build());

            log.info("Preferencia MP creada: id={}, pedido={}", preference.getId(), pedido.getId());

            return new PreferenciaResponse(preference.getId(), preference.getInitPoint());

        } catch (MPApiException e) {
            log.error("Error API MP pedido {}: status={} body={}", pedido.getId(), e.getApiResponse().getStatusCode(), e.getApiResponse().getContent());
            throw new EcommerceException("Error al conectar con MercadoPago: " + e.getApiResponse().getContent());
        } catch (Exception e) {
            log.error("Error al crear preferencia MP para pedido {}: {}", pedido.getId(), e.getMessage());
            throw new EcommerceException("Error al conectar con MercadoPago: " + e.getMessage());
        }
    }

    public Payment obtenerPago(Long paymentId) {
        try {
            PaymentClient client = new PaymentClient();
            return client.get(paymentId);
        } catch (Exception e) {
            log.error("Error al obtener pago MP {}: {}", paymentId, e.getMessage());
            throw new EcommerceException("Error al verificar el pago en MercadoPago: " + e.getMessage());
        }
    }

    private PreferenceItemRequest mapItem(ItemPedido item) {
        BigDecimal unitPrice = BigDecimal.valueOf(item.getProducto().getPrecio());

        String itemId = item.getVariante() != null
                ? item.getVariante().getId().toString()
                : item.getProducto().getId().toString();

        return PreferenceItemRequest.builder()
                .id(itemId)
                .title(item.getProducto().getNombre())
                .description(item.getProducto().getDescripcion())
                .quantity(item.getCantidad())
                .currencyId("ARS")
                .unitPrice(unitPrice)
                .build();
    }
}
