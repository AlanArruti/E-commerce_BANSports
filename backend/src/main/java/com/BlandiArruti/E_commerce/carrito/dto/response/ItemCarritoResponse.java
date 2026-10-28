package com.BlandiArruti.E_commerce.carrito.dto.response;

import com.BlandiArruti.E_commerce.producto.dto.response.ProductoResponse;
import com.BlandiArruti.E_commerce.producto.dto.response.VarianteResponse;

public record ItemCarritoResponse(
        Long idItemCarrito,
        ProductoResponse producto,
        VarianteResponse variante,
        Integer cantidad,
        Double subtotal
) {}
