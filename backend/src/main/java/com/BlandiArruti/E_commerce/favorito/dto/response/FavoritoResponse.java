package com.BlandiArruti.E_commerce.favorito.dto.response;

import com.BlandiArruti.E_commerce.producto.dto.response.ProductoResponse;

import java.time.LocalDateTime;

public record FavoritoResponse(
        ProductoResponse producto,
        LocalDateTime fechaAgregado
) {}
