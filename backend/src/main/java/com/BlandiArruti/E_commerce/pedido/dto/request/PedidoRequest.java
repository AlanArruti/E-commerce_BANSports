package com.BlandiArruti.E_commerce.pedido.dto.request;

import com.BlandiArruti.E_commerce.cotizacion.dto.request.OpcionEnvioRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PedidoRequest(
        // Ignorado para rol CLIENTE (se extrae del JWT). Requerido para ADMIN.
        Long idCliente,

        Long idDireccion,

        // Requerida si hay dirección. El precio no viaja: lo cotiza el back.
        @Valid
        OpcionEnvioRequest opcionEnvio,

        @NotEmpty(message = "El pedido debe tener al menos un ítem.")
        @Valid
        List<ItemPedidoRequest> items,

        // Opcional. Se valida y se aplica al crear el pedido; el descuento lo calcula el back.
        @Size(max = 30, message = "El código de cupón no puede superar los 30 caracteres.")
        String codigoCupon
) {}
