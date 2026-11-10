package com.BlandiArruti.E_commerce.producto.dto.response;

import java.util.Map;

public record VarianteResponse(
        Long id,
        String uuidVariante,
        Map<String, String> atributos,
        Integer stock,
        boolean activo,
        Long idProducto,
        /** Precio propio cargado en el panel, sin descuento; null si usa el del producto. */
        Double precio,
        /** Lo que paga el cliente por una unidad: precio de la variante (o del producto) con la oferta aplicada. */
        Double precioFinal,
        /** URL de la foto propia de la variante, o null si usa la del producto. */
        String imagenUrl
) {}
