package com.BlandiArruti.E_commerce.pedido.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PedidoRequest(
        // Ignorado para rol CLIENTE (se extrae del JWT). Requerido para ADMIN.
        Long idCliente,

        Long idDireccion,

        @NotEmpty(message = "El pedido debe tener al menos un ítem.")
        @Valid
        List<ItemPedidoRequest> items
) {}
