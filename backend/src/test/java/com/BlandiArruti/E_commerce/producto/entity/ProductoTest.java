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

    // ─── precioUnitario (SCRUM-154): precio por variante ──────────────────────

    @Test
    void precioUnitario_varianteConPrecioPropio_usaElDeLaVariante() {
        Producto producto = Producto.builder().precio(30000.0).descuento(0).build();
        Variante dosKilos = Variante.builder().precio(55000.0).build();

        assertThat(producto.precioUnitario(dosKilos)).isEqualTo(55000.0);
    }

    @Test
    void precioUnitario_varianteSinPrecio_usaElDelProducto() {
        Producto producto = Producto.builder().precio(30000.0).descuento(0).build();

        assertThat(producto.precioUnitario(Variante.builder().build())).isEqualTo(30000.0);
        assertThat(producto.precioUnitario(null)).isEqualTo(30000.0);
    }

    @Test
    void precioUnitario_ofertaDelProducto_seAplicaAlPrecioDeLaVariante() {
        Producto producto = Producto.builder().precio(30000.0).descuento(20).build();
        Variante dosKilos = Variante.builder().precio(55000.0).build();

        assertThat(producto.precioUnitario(dosKilos)).isEqualTo(44000.0);
        assertThat(producto.getPrecioConDescuento()).as("el producto sigue con su precio").isEqualTo(24000.0);
    }

    @Test
    void precioConDescuento_descuentoCeroDevuelveElPrecioBase() {
        Producto producto = Producto.builder()
                .precio(15000.0)
                .descuento(0)
                .build();

        assertThat(producto.getPrecioConDescuento()).isEqualTo(15000.0);
    }

    @Test
    void recalcularPrecioFinal_descuentoVacioQuedaEnCero() {
        Producto producto = Producto.builder()
                .precio(15000.0)
                .descuento(null)
                .build();

        producto.recalcularPrecioFinal();

        assertThat(producto.getDescuento()).isZero();
        assertThat(producto.getPrecioFinal()).isEqualTo(15000.0);
    }
}
