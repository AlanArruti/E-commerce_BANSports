package com.BlandiArruti.E_commerce.producto.controller;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductoControllerTest {

    @Test
    void mayorDescuento_losSinDescuentoVanAlFinalYDesempataPorId() {
        Sort orden = ProductoController.ordenConNulosAlFinal(Sort.Direction.DESC, "descuento");

        List<Sort.Order> ordenes = orden.toList();
        assertThat(ordenes).hasSize(2);
        assertThat(ordenes.get(0).getProperty()).isEqualTo("descuento");
        assertThat(ordenes.get(0).getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(ordenes.get(0).getNullHandling()).isEqualTo(Sort.NullHandling.NULLS_LAST);
        assertThat(ordenes.get(1).getProperty()).isEqualTo("id");
        assertThat(ordenes.get(1).getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void porId_noAgregaDesempate() {
        Sort orden = ProductoController.ordenConNulosAlFinal(Sort.Direction.ASC, "id");

        assertThat(orden.toList()).hasSize(1);
    }
}
