package com.BlandiArruti.E_commerce.dolar.dto.response;

import java.time.OffsetDateTime;

/** Un ajuste de precios hecho por la variación del dólar (historial del admin). */
public record AjustePreciosResponse(
        String casa,
        Double venta,
        Double porcentajeAjuste,
        Integer productosAjustados,
        OffsetDateTime fechaAjuste
) {}
