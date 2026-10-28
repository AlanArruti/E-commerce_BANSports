package com.BlandiArruti.E_commerce.cotizacion.dto.response;

public record CotizacionEnvioResponse(
        String transportista,
        String tipoEntrega,
        String servicio,
        Double precio,
        Integer plazoMinimoDias,
        Integer plazoMaximoDias
) {}
