package com.BlandiArruti.E_commerce.dolar.service;

import com.BlandiArruti.E_commerce.dolar.dto.response.ActualizacionDolarResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Consulta el dólar al arrancar la app (a los pocos segundos) y después cada dolar.intervalo.
 * Se apaga con DOLAR_ACTUALIZACION_AUTOMATICA=false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "dolar", name = "actualizacion-automatica", havingValue = "true", matchIfMissing = true)
public class DolarScheduler {

    private final DolarService dolarService;

    @Scheduled(initialDelayString = "PT15S", fixedDelayString = "${dolar.intervalo:PT30M}")
    public void actualizar() {
        try {
            ActualizacionDolarResponse respuesta = dolarService.actualizar(false);
            log.debug("Cotización del dólar: {} ({})", respuesta.cotizacion().venta(), respuesta.resultado());
        } catch (RuntimeException e) {
            // Si la API está caída se reintenta en la próxima vuelta; los precios quedan como estaban.
            log.warn("No se pudo actualizar la cotización del dólar: {}", e.getMessage());
        }
    }
}
