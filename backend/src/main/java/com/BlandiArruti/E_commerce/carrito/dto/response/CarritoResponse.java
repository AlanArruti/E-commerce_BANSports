package com.BlandiArruti.E_commerce.carrito.dto.response;

import java.util.List;

public record CarritoResponse(
        Long id,
        String uuidCarrito,
        List<ItemCarritoResponse> items,
        Double total
) {}
