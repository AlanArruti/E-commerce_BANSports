package com.BlandiArruti.E_commerce.factura.dto.response;

/** PDF de la factura listo para descargar. */
public record FacturaPdfResponse(
        String nombreArchivo,
        byte[] contenido
) {}
