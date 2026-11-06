package com.BlandiArruti.E_commerce.cotizacion.dto.response;

public record TarifaEnvioResponse(
        Long id,
        String uuidTarifa,
        String provincia,
        Double precio,
        Integer plazoMinimoDias,
        Integer plazoMaximoDias
) {}
