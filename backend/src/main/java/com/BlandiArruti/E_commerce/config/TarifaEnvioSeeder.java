package com.BlandiArruti.E_commerce.config;

import com.BlandiArruti.E_commerce.cotizacion.entity.TarifaEnvio;
import com.BlandiArruti.E_commerce.cotizacion.repository.TarifaEnvioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Precarga la tabla de tarifas de envío (respaldo) si está vacía, para que la
 * cotización nunca quede sin respuesta aunque Correo Argentino no esté configurado.
 * El admin puede editarlas después por la API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TarifaEnvioSeeder implements CommandLineRunner {

    private final TarifaEnvioRepository tarifaEnvioRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (tarifaEnvioRepository.count() > 0) {
            return;
        }

        List<TarifaEnvio> tarifas = List.of(
                TarifaEnvio.builder()
                        .provincia(null)
                        .precio(6000.0)
                        .plazoMinimoDias(3)
                        .plazoMaximoDias(5)
                        .build(),
                TarifaEnvio.builder()
                        .provincia("Buenos Aires")
                        .precio(4500.0)
                        .plazoMinimoDias(3)
                        .plazoMaximoDias(4)
                        .build(),
                TarifaEnvio.builder()
                        .provincia("Ciudad Autónoma de Buenos Aires")
                        .precio(3000.0)
                        .plazoMinimoDias(2)
                        .plazoMaximoDias(3)
                        .build(),
                TarifaEnvio.builder()
                        .provincia("Córdoba")
                        .precio(6000.0)
                        .plazoMinimoDias(4)
                        .plazoMaximoDias(5)
                        .build(),
                TarifaEnvio.builder()
                        .provincia("Santa Fe")
                        .precio(5500.0)
                        .plazoMinimoDias(3)
                        .plazoMaximoDias(4)
                        .build()
        );

        tarifaEnvioRepository.saveAll(tarifas);
        log.info("Se precargaron {} tarifas de envío de respaldo.", tarifas.size());
    }
}
