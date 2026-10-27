package com.BlandiArruti.E_commerce.pedido.dto.response;

import com.BlandiArruti.E_commerce.cliente.dto.response.ClienteResponse;
import com.BlandiArruti.E_commerce.envio.dto.response.EnvioResponse;
import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.factura.dto.response.FacturaResponse;

import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        String uuidPedido,
        LocalDateTime fechaCreacion,
        ClienteResponse cliente,
        List<ItemPedidoResponse> items,
        /** Suma de los ítems. Se calcula al mapear, no está persistido. */
        Double total,
        EstadoPedido estado,
        EnvioResponse envio,
        FacturaResponse factura
) {}
