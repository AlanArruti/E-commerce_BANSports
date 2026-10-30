package com.BlandiArruti.E_commerce.producto.dto.response;

import com.BlandiArruti.E_commerce.categoria.dto.response.CategoriaResponse;

import java.util.List;

public record ProductoResponse(
        Long id,
        String uuidProducto,
        String nombre,
        String descripcion,
        Double precioBase,
        Integer descuento,
        Double precioFinal,
        boolean activo,
        /** URL de la imagen, o null si el producto no tiene una cargada. */
        String imagenUrl,
        CategoriaResponse categoria,
        List<VarianteResponse> variantes
) {}
