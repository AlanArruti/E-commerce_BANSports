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
        /** Costo de envío persistido al confirmar el checkout. */
        Double costoEnvio,
        /** Lo que descontó el cupón sobre los productos (0 si no usó). */
        Double descuentoCupon,
        /** Código del cupón usado, o null. */
        String codigoCupon,
        /** Ítems menos el cupón, más el envío. Se calcula al mapear, no está persistido. */
        Double total,
        EstadoPedido estado,
        EnvioResponse envio,
        FacturaResponse factura
) {}
