package com.BlandiArruti.E_commerce.geo.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CodigoPostalTest {

    private static final String MDP = "Mar del Plata (Buenos Aires)";

    @ParameterizedTest
    @ValueSource(strings = {"7600", "B7600ABC", "b7600abc", " 1425 "})
    void formatoValido_aceptaCuatroNumerosOCpa(String codigo) {
        assertThat(CodigoPostal.formatoValido(codigo)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "-1", "7", "76000", "B7600", "7600ABC", "B76OOABC", ""})
    void formatoValido_rechazaLoDemas(String codigo) {
        assertThat(CodigoPostal.formatoValido(codigo)).isFalse();
    }

    @Test
    void normalizar_sacaEspaciosYPasaAMayusculas() {
        assertThat(CodigoPostal.normalizar(" b7600abc ")).isEqualTo("B7600ABC");
    }

    @Test
    void cpaDeLaMismaProvincia_noContradice() {
        assertThat(CodigoPostal.contradiccionConProvincia("B7600ABC", "06", MDP)).isEmpty();
    }

    @Test
    void cpaDeOtraProvincia_diceDeCualEs() {
        assertThat(CodigoPostal.contradiccionConProvincia("X5000ABC", "06", MDP))
                .hasValueSatisfying(m -> assertThat(m).contains("Córdoba").contains(MDP));
    }

    @Test
    void cuatroNumerosFueraDeCaba_noSePuedeControlarYPasa() {
        assertThat(CodigoPostal.contradiccionConProvincia("7600", "06", MDP)).isEmpty();
        assertThat(CodigoPostal.contradiccionConProvincia("5000", "06", MDP)).isEmpty();
    }

    @Test
    void codigoDeCabaEnOtraProvincia_seRechaza() {
        assertThat(CodigoPostal.contradiccionConProvincia("1425", "06", MDP))
                .hasValueSatisfying(m -> assertThat(m).contains("Ciudad de Buenos Aires"));
    }

    @Test
    void enCaba_soloValenDel1000Al1499() {
        String caba = "Ciudad de Buenos Aires (Ciudad Autónoma de Buenos Aires)";
        assertThat(CodigoPostal.contradiccionConProvincia("1425", "02", caba)).isEmpty();
        assertThat(CodigoPostal.contradiccionConProvincia("C1425ABC", "02", caba)).isEmpty();
        assertThat(CodigoPostal.contradiccionConProvincia("7600", "02", caba)).isPresent();
    }
}
