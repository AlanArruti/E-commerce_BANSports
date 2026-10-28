package com.BlandiArruti.E_commerce.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService();
    }

    @Test
    void loginFallido_menosDe5Intentos_noBloqueaCuenta() {
        service.loginFallido("a@test.com");
        service.loginFallido("a@test.com");
        service.loginFallido("a@test.com");
        service.loginFallido("a@test.com");

        assertThat(service.estaBloqueado("a@test.com")).isFalse();
    }

    @Test
    void loginFallido_exactamente5Intentos_bloqueaCuenta() {
        for (int i = 0; i < 5; i++) {
            service.loginFallido("a@test.com");
        }

        assertThat(service.estaBloqueado("a@test.com")).isTrue();
    }

    @Test
    void loginFallido_masDe5Intentos_sigueBloqueado() {
        for (int i = 0; i < 8; i++) {
            service.loginFallido("a@test.com");
        }

        assertThat(service.estaBloqueado("a@test.com")).isTrue();
    }

    @Test
    void loginExitoso_limpiaCuentaBloqueada() {
        for (int i = 0; i < 5; i++) {
            service.loginFallido("a@test.com");
        }
        assertThat(service.estaBloqueado("a@test.com")).isTrue();

        service.loginExitoso("a@test.com");

        assertThat(service.estaBloqueado("a@test.com")).isFalse();
    }

    @Test
    void loginExitoso_reiniciaContadorParaBloqueoFuturo() {
        service.loginFallido("a@test.com");
        service.loginFallido("a@test.com");
        service.loginExitoso("a@test.com");

        // 4 intentos después del reset no deberían bloquear
        service.loginFallido("a@test.com");
        service.loginFallido("a@test.com");
        service.loginFallido("a@test.com");
        service.loginFallido("a@test.com");

        assertThat(service.estaBloqueado("a@test.com")).isFalse();
    }

    @Test
    void estaBloqueado_usuarioSinHistorial_devuelveFalse() {
        assertThat(service.estaBloqueado("nuevo@test.com")).isFalse();
    }

    @Test
    void minutosRestantes_cuentaBloqueada_devuelveValorPositivo() {
        for (int i = 0; i < 5; i++) {
            service.loginFallido("a@test.com");
        }

        long minutos = service.minutosRestantes("a@test.com");

        assertThat(minutos).isGreaterThanOrEqualTo(1).isLessThanOrEqualTo(15);
    }

    @Test
    void minutosRestantes_sinBloqueo_devuelveCero() {
        assertThat(service.minutosRestantes("sin@bloqueo.com")).isEqualTo(0);
    }
}
