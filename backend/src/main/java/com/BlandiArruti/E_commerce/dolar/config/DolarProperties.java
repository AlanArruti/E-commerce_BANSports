package com.BlandiArruti.E_commerce.dolar.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuración de la cotización del dólar (RF01). La cotización sale de DolarApi
 * (dolarapi.com), gratis y sin key. Los precios de los productos siguen en pesos: cuando
 * el dólar se mueve lo suficiente, se ajustan todos en el mismo porcentaje.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "dolar")
public class DolarProperties {

    /** URL base de DolarApi. */
    private String baseUrl = "https://dolarapi.com";

    /** Qué dólar se usa: oficial, blue, bolsa (MEP), contadoconliqui, mayorista, cripto, tarjeta. */
    private String casa = "oficial";

    /** Si está en false no se consulta sola: solo con POST /api/v1/dolar/actualizar. */
    private boolean actualizacionAutomatica = true;

    /** Cada cuánto se consulta la cotización. */
    private Duration intervalo = Duration.ofMinutes(30);

    /**
     * Variación mínima (en %) contra la cotización con la que se ajustaron los precios por última
     * vez para volver a ajustarlos. Evita mover todos los precios unos centavos cada día.
     */
    private double ajusteMinimoPorcentaje = 1.0;

    /**
     * Variación máxima (en %) que se aplica sola. Si el dólar salta más que esto, la cotización se
     * guarda pero los precios no se tocan: puede ser un dato erróneo de la API, y lo decide el admin
     * con POST /api/v1/dolar/actualizar?forzar=true.
     */
    private double variacionMaximaPorcentaje = 20.0;
}
