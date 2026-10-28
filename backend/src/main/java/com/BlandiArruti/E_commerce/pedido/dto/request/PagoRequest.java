package com.BlandiArruti.E_commerce.pedido.dto.request;

import com.BlandiArruti.E_commerce.enums.TipoFactura;
import jakarta.validation.constraints.NotNull;

public record PagoRequest(
        @NotNull(message = "El tipo de factura es obligatorio.")
        TipoFactura tipoFactura
) {}
