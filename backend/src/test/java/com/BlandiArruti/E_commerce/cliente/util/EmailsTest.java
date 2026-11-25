package com.BlandiArruti.E_commerce.cliente.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailsTest {

    @Test
    void normalizar_quitaEspaciosYPasaAMinusculas() {
        assertThat(Emails.normalizar("  Juan.Perez@Gmail.COM ")).isEqualTo("juan.perez@gmail.com");
        assertThat(Emails.normalizar(null)).isNull();
    }

    @Test
    void usuarioGmail_ignoraPuntosEtiquetaYMayusculas() {
        assertThat(Emails.usuarioGmail("Juan.Perez+compras@Gmail.com")).isEqualTo("juanperez");
        assertThat(Emails.usuarioGmail("juanperez@googlemail.com")).isEqualTo("juanperez");
    }

    @Test
    void usuarioGmail_otrosDominiosNoSeTocan() {
        // Fuera de Gmail, "juan.perez" y "juanperez" pueden ser personas distintas.
        assertThat(Emails.usuarioGmail("juan.perez@hotmail.com")).isNull();
        assertThat(Emails.usuarioGmail("sin-arroba")).isNull();
        assertThat(Emails.usuarioGmail(null)).isNull();
    }
}
