package com.BlandiArruti.E_commerce.factura.dto.response;

import com.BlandiArruti.E_commerce.enums.TipoFactura;

public record FacturaResponse(
        Long id,
        String uuidFactura,
        TipoFactura tipo,
        Double precioTotal,
        Long idPedido
) {}
