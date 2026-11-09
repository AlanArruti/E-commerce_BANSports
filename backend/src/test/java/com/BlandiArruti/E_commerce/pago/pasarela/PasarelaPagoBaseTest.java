package com.BlandiArruti.E_commerce.pago.pasarela;

import com.BlandiArruti.E_commerce.enums.EstadoPago;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.exception.ServicioExternoException;
import com.BlandiArruti.E_commerce.pago.pasarela.dto.CobroIniciado;
import com.BlandiArruti.E_commerce.pago.pasarela.dto.ResultadoCobro;
import com.BlandiArruti.E_commerce.pago.pasarela.dto.SolicitudCobro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba el algoritmo común de la clase padre (Template Method) con una pasarela inventada.
 * Muestra también lo que hace falta para sumar una pasarela nueva: extender la base y
 * completar tres métodos.
 */
class PasarelaPagoBaseTest {

    /** Pasarela de mentira: guarda lo que le pidieron y devuelve lo que el test configure. */
    static class PasarelaFalsa extends PasarelaPagoBase {
        boolean disponible = true;
        CobroIniciado respuestaCrear = new CobroIniciado("EXT-1", "https://pasarela.falsa/pagar/EXT-1");
        ResultadoCobro respuestaBuscar;
        RuntimeException errorAlLeer;
        SolicitudCobro ultimaSolicitud;

        @Override public String codigo() { return "falsa"; }
        @Override public String nombre() { return "Pasarela Falsa"; }
        @Override public boolean estaDisponible() { return disponible; }

        @Override
        protected CobroIniciado crearCobro(SolicitudCobro solicitud) {
            ultimaSolicitud = solicitud;
            return respuestaCrear;
        }

        @Override
        protected ResultadoCobro buscarCobro(String idExterno) {
            return respuestaBuscar;
        }

        @Override
        protected Optional<String> extraerIdDeNotificacion(Map<String, Object> cuerpo, Map<String, String> parametros) {
            if (errorAlLeer != null) {
                throw errorAlLeer;
            }
            return Optional.ofNullable(parametros.get("id"));
        }
    }

    private final PasarelaFalsa pasarela = new PasarelaFalsa();

    private SolicitudCobro solicitud(String... montos) {
        List<SolicitudCobro.LineaCobro> lineas = java.util.Arrays.stream(montos)
                .map(m -> new SolicitudCobro.LineaCobro("P1", "Proteína", 1, new BigDecimal(m)))
                .toList();
        return new SolicitudCobro("15", "Pedido #3", lineas, "http://localhost:4200/pago/resultado?pago=15");
    }

    // ─── iniciarCobro ────────────────────────────────────────────────────────

    @Test
    void iniciarCobro_valido_delegaEnLaHijaYDevuelveLaUrl() {
        CobroIniciado cobro = pasarela.iniciarCobro(solicitud("1000.00", "500.50"));

        assertThat(cobro.urlPago()).isEqualTo("https://pasarela.falsa/pagar/EXT-1");
        assertThat(pasarela.ultimaSolicitud.total()).isEqualByComparingTo("1500.50");
    }

    @Test
    void iniciarCobro_pasarelaSinCredenciales_noLlamaALaHija() {
        pasarela.disponible = false;

        assertThatThrownBy(() -> pasarela.iniciarCobro(solicitud("1000.00")))
                .isInstanceOf(ServicioExternoException.class)
                .hasMessageContaining("Pasarela Falsa");
        assertThat(pasarela.ultimaSolicitud).isNull();
    }

    @Test
    void iniciarCobro_sinNadaParaCobrar_rechaza() {
        assertThatThrownBy(() -> pasarela.iniciarCobro(solicitud()))
                .isInstanceOf(EcommerceException.class);
        assertThatThrownBy(() -> pasarela.iniciarCobro(solicitud("0.00")))
                .isInstanceOf(EcommerceException.class);
    }

    @Test
    void iniciarCobro_laHijaNoDevuelveUrl_fallaClaro() {
        pasarela.respuestaCrear = new CobroIniciado("EXT-1", null);

        assertThatThrownBy(() -> pasarela.iniciarCobro(solicitud("1000.00")))
                .isInstanceOf(ServicioExternoException.class);
    }

    // ─── consultarCobro ──────────────────────────────────────────────────────

    @Test
    void consultarCobro_devuelveLoQueTraduceLaHija() {
        pasarela.respuestaBuscar = new ResultadoCobro("EXT-1", EstadoPago.APROBADO, new BigDecimal("1000"), "15");

        assertThat(pasarela.consultarCobro("EXT-1").estado()).isEqualTo(EstadoPago.APROBADO);
    }

    @Test
    void consultarCobro_sinId_rechazaSinLlamarALaHija() {
        assertThatThrownBy(() -> pasarela.consultarCobro(" "))
                .isInstanceOf(EcommerceException.class);
    }

    @Test
    void consultarCobro_laHijaNoDevuelveEstado_fallaClaro() {
        pasarela.respuestaBuscar = new ResultadoCobro("EXT-1", null, null, "15");

        assertThatThrownBy(() -> pasarela.consultarCobro("EXT-1"))
                .isInstanceOf(ServicioExternoException.class);
    }

    // ─── leerNotificacion ────────────────────────────────────────────────────

    @Test
    void leerNotificacion_avisoValido_devuelveElId() {
        assertThat(pasarela.leerNotificacion(null, Map.of("id", "EXT-9"))).contains("EXT-9");
    }

    @Test
    void leerNotificacion_avisoQueRompeLaHija_seDescartaSinLanzar() {
        pasarela.errorAlLeer = new IllegalStateException("formato raro");

        assertThat(pasarela.leerNotificacion(Map.of(), Map.of())).isEmpty();
    }

    // ─── RegistroPasarelas ───────────────────────────────────────────────────

    @Test
    void registro_encuentraLaPasarelaPorCodigoYSoloOfreceLasDisponibles() {
        PasarelaFalsa apagada = new PasarelaFalsa() {
            @Override public String codigo() { return "apagada"; }
        };
        apagada.disponible = false;
        RegistroPasarelas registro = new RegistroPasarelas(List.of(pasarela, apagada));

        assertThat(registro.obtener("falsa")).isSameAs(pasarela);
        assertThat(registro.disponibles()).containsExactly(pasarela);
        assertThatThrownBy(() -> registro.obtener("inexistente"))
                .isInstanceOf(EntidadNoEncontradaException.class);
    }

    @Test
    void registro_dosPasarelasConElMismoCodigo_fallaAlArrancar() {
        assertThatThrownBy(() -> new RegistroPasarelas(List.of(pasarela, new PasarelaFalsa())))
                .isInstanceOf(IllegalStateException.class);
    }
}
