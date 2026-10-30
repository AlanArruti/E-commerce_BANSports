package com.BlandiArruti.E_commerce.producto.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductoTest {

    @Test
    void precioConDescuento_aplicaElPorcentajeYRedondea() {
        Producto producto = Producto.builder()
                .precio(42999.99)
                .descuento(30)
                .build();

        assertThat(producto.getPrecioConDescuento()).isEqualTo(30099.99);
    }

    @Test
    void precioConDescuento_sinOfertaDevuelveElPrecioBase() {
        Producto producto = Producto.builder()
                .precio(15000.0)
                .descuento(null)
                .build();

        assertThat(producto.getPrecioConDescuento()).isEqualTo(15000.0);
    }

    @Test
    void precioConDescuento_descuentoCeroDevuelveElPrecioBase() {
        Producto producto = Producto.builder()
                .precio(15000.0)
                .descuento(0)
                .build();

        assertThat(producto.getPrecioConDescuento()).isEqualTo(15000.0);
    }
}
