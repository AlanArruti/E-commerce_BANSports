package com.BlandiArruti.E_commerce.dolar.dto.response;

import java.time.OffsetDateTime;

/**
 * Cotización actual del dólar, para el admin y el cliente.
 *
 * @param cotizacionReferencia valor del dólar que tienen incorporado los precios actuales
 * @param fechaUltimoAjuste    cuándo se ajustaron los precios por última vez
 * @param variacionPorcentaje  cuánto se movió el dólar desde ese ajuste (positivo = subió)
 * @param ajusteMinimoPorcentaje      desde qué variación se ajustan los precios
 * @param variacionMaximaPorcentaje   desde qué variación el ajuste lo tiene que confirmar el admin
 */
public record CotizacionDolarResponse(
        String casa,
        Double compra,
        Double venta,
        OffsetDateTime fechaActualizacion,
        OffsetDateTime fechaConsulta,
        Double cotizacionReferencia,
        OffsetDateTime fechaUltimoAjuste,
        Double variacionPorcentaje,
        double ajusteMinimoPorcentaje,
        double variacionMaximaPorcentaje
) {}
