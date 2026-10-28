package com.BlandiArruti.E_commerce.pedido.dto.response;

import com.BlandiArruti.E_commerce.producto.dto.response.ProductoResponse;
import com.BlandiArruti.E_commerce.producto.dto.response.VarianteResponse;

public record ItemPedidoResponse(
        Long idItemPedido,
        ProductoResponse producto,
        VarianteResponse variante,
        Integer cantidad,
        Double precioTotalProducto
) {}
