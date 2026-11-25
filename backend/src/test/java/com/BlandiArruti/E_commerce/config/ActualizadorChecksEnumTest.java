package com.BlandiArruti.E_commerce.config;

import com.BlandiArruti.E_commerce.enums.EstadoPago;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ActualizadorChecksEnumTest {

    @Test
    void armaElCheckConTodosLosValoresDelEnum_incluidoReembolsado() {
        List<String> sql = ActualizadorChecksEnum.sentencias(
                new ActualizadorChecksEnum.ColumnaEnum("pagos", "estado", EstadoPago.class));

        assertThat(sql).containsExactly(
                "ALTER TABLE IF EXISTS pagos DROP CONSTRAINT IF EXISTS pagos_estado_check",
                "ALTER TABLE IF EXISTS pagos ADD CONSTRAINT pagos_estado_check "
                        + "CHECK (estado IN ('CREADO', 'PENDIENTE', 'APROBADO', 'RECHAZADO', 'REEMBOLSADO'))");
    }

    @Test
    void siLaBaseFalla_noFrenaElArranque() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        doThrow(new RuntimeException("sin permisos")).when(jdbc).execute(anyString());

        new ActualizadorChecksEnum(jdbc).run(null);

        verify(jdbc, atLeastOnce()).execute(anyString());
    }
}
