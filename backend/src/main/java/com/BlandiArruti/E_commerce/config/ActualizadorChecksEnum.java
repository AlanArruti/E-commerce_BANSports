package com.BlandiArruti.E_commerce.config;

import com.BlandiArruti.E_commerce.enums.EstadoPago;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Hibernate le pone a cada columna enum un CHECK con los valores permitidos, pero ddl-auto=update
 * no lo actualiza cuando el enum suma un valor: en una base que ya existía, guardar el valor nuevo
 * falla. Al arrancar, este componente vuelve a armar esos CHECK con los valores actuales.
 *
 * Sumar acá cada columna enum a la que se le agregue un valor (RF04: EstadoPago.REEMBOLSADO).
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class ActualizadorChecksEnum implements ApplicationRunner {

    /** Tabla, columna y enum. El nombre del CHECK es el que genera Hibernate: tabla_columna_check. */
    record ColumnaEnum(String tabla, String columna, Class<? extends Enum<?>> tipo) {}

    static final List<ColumnaEnum> COLUMNAS = List.of(
            new ColumnaEnum("pagos", "estado", EstadoPago.class)
    );

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        for (ColumnaEnum c : COLUMNAS) {
            try {
                for (String sql : sentencias(c)) {
                    jdbcTemplate.execute(sql);
                }
            } catch (RuntimeException e) {
                // No frena el arranque: en el peor caso queda el CHECK viejo, como antes.
                log.warn("No se pudo actualizar el CHECK de {}.{}: {}", c.tabla(), c.columna(), e.getMessage());
            }
        }
    }

    /** DROP + ADD del CHECK con todos los valores del enum. Los nombres vienen de esta clase, no del usuario. */
    static List<String> sentencias(ColumnaEnum c) {
        String restriccion = c.tabla() + "_" + c.columna() + "_check";
        String valores = Arrays.stream(c.tipo().getEnumConstants())
                .map(v -> "'" + v.name() + "'")
                .collect(Collectors.joining(", "));
        return List.of(
                "ALTER TABLE IF EXISTS " + c.tabla() + " DROP CONSTRAINT IF EXISTS " + restriccion,
                "ALTER TABLE IF EXISTS " + c.tabla() + " ADD CONSTRAINT " + restriccion
                        + " CHECK (" + c.columna() + " IN (" + valores + "))");
    }
}
