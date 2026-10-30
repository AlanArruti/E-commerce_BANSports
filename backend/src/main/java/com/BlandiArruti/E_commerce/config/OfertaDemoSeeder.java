package com.BlandiArruti.E_commerce.config;

import com.BlandiArruti.E_commerce.producto.entity.Producto;
import com.BlandiArruti.E_commerce.producto.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Carga ofertas de ejemplo sobre una base que ya existe.
 * DataLoader solo corre con la base vacía; este seeder cubre el caso de Docker
 * con el volumen ya creado. Se desactiva con OFERTAS_DEMO=false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OfertaDemoSeeder implements CommandLineRunner {

    private static final Map<String, Integer> DESCUENTOS_DEMO = Map.of(
            "Whey Protein Concentrada", 30,
            "Creatina Monohidrato", 25,
            "Pre-entreno Explosive", 20,
            "Caseína Micelar", 15,
            "Omega 3", 12,
            "Glutamina", 10
    );

    private final ProductoRepository productoRepository;

    @Value("${app.ofertas-demo.habilitadas:true}")
    private boolean habilitado;

    @Override
    @Transactional
    public void run(String... args) {
        if (!habilitado || productoRepository.existsByDescuentoGreaterThan(0)) {
            return;
        }

        List<Producto> productos = productoRepository.findAllByActivoTrue();
        if (productos.isEmpty()) {
            return;
        }

        int asignados = 0;
        for (Producto producto : productos) {
            Integer descuento = DESCUENTOS_DEMO.get(producto.getNombre());
            if (descuento != null) {
                producto.setDescuento(descuento);
                asignados++;
            } else if (producto.getDescuento() == null) {
                producto.setDescuento(0);
            }
            producto.recalcularPrecioFinal();
        }

        // Si la base tiene productos propios con otros nombres, igual dejamos algunas ofertas.
        if (asignados == 0) {
            int[] porcentajes = {30, 25, 20, 15, 10};
            for (int i = 0; i < Math.min(porcentajes.length, productos.size()); i++) {
                productos.get(i).setDescuento(porcentajes[i]);
                productos.get(i).recalcularPrecioFinal();
            }
        }

        productoRepository.saveAll(productos);
        log.info("Se cargaron ofertas de ejemplo en los productos.");
    }
}
