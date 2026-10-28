package com.BlandiArruti.E_commerce.pedido.dto.request;

import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import jakarta.validation.constraints.NotNull;

public record EstadoPedidoRequest(
        @NotNull(message = "El estado es obligatorio.")
        EstadoPedido estado
) {}
