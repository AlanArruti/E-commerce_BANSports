package com.BlandiArruti.E_commerce.producto.dto.response;

public record ImagenProductoResponse(
        Long idProducto,
        String nombreArchivo,
        String contentType,
        long tamanioBytes,
        String url
) {}
