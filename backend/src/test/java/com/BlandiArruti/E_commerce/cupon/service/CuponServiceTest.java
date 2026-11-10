package com.BlandiArruti.E_commerce.cupon.service;

import com.BlandiArruti.E_commerce.carrito.repository.CarritoRepository;
import com.BlandiArruti.E_commerce.cupon.dto.request.CuponRequest;
import com.BlandiArruti.E_commerce.cupon.dto.response.CuponResponse;
import com.BlandiArruti.E_commerce.cupon.entity.Cupon;
import com.BlandiArruti.E_commerce.cupon.repository.CuponRepository;
import com.BlandiArruti.E_commerce.enums.TipoCupon;
import com.BlandiArruti.E_commerce.exception.ConflictoException;
import com.BlandiArruti.E_commerce.exception.CuponInvalidoException;
import com.BlandiArruti.E_commerce.exception.DuplicadoException;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuponServiceTest {

    @Mock CuponRepository cuponRepository;
    @Mock CarritoRepository carritoRepository;

    @InjectMocks
    CuponService cuponService;

    private static final LocalDate HOY = LocalDate.of(2026, 11, 9);

    private Cupon.CuponBuilder porcentaje(double valor) {
        return Cupon.builder().id(1L).codigo("VERANO").tipo(TipoCupon.PORCENTAJE).valor(valor);
    }

    // ─── calcularDescuento: las reglas del cupón ─────────────────────────────

    @Test
    void porcentaje_descuentaSobreElSubtotalYRedondeaACentavos() {
        Cupon cupon = porcentaje(15).build();

        assertThat(cuponService.calcularDescuento(cupon, 33333.33, HOY)).isEqualTo(5000.0);
        assertThat(cuponService.calcularDescuento(cupon, 1000.01, HOY)).isEqualTo(150.0);
    }

    @Test
    void montoFijo_descuentaElMonto() {
        Cupon cupon = Cupon.builder().codigo("MENOS5000").tipo(TipoCupon.MONTO_FIJO).valor(5000.0).build();

        assertThat(cuponService.calcularDescuento(cupon, 20000.0, HOY)).isEqualTo(5000.0);
    }

    @Test
    void montoFijo_nuncaDescuentaMasQueElSubtotal() {
        Cupon cupon = Cupon.builder().codigo("MENOS5000").tipo(TipoCupon.MONTO_FIJO).valor(5000.0).build();

        assertThat(cuponService.calcularDescuento(cupon, 3200.0, HOY)).isEqualTo(3200.0);
    }

    @Test
    void vencido_loRechazaConLaFecha() {
        Cupon cupon = porcentaje(10).vigenteHasta(HOY.minusDays(1)).build();

        assertThatThrownBy(() -> cuponService.calcularDescuento(cupon, 50000.0, HOY))
                .isInstanceOf(CuponInvalidoException.class)
                .hasMessage("El cupón VERANO venció el 08/11/2026.");
    }

    @Test
    void elUltimoDiaDeVigencia_todaviaSirve() {
        Cupon cupon = porcentaje(10).vigenteHasta(HOY).build();

        assertThat(cuponService.calcularDescuento(cupon, 1000.0, HOY)).isEqualTo(100.0);
    }

    @Test
    void todaviaNoEmpezo_loRechaza() {
        Cupon cupon = porcentaje(10).vigenteDesde(HOY.plusDays(3)).build();

        assertThatThrownBy(() -> cuponService.calcularDescuento(cupon, 50000.0, HOY))
                .isInstanceOf(CuponInvalidoException.class)
                .hasMessageContaining("se puede usar desde el 12/11/2026");
    }

    @Test
    void agotado_loRechaza() {
        Cupon cupon = porcentaje(10).usosMaximos(50).usos(50).build();

        assertThatThrownBy(() -> cuponService.calcularDescuento(cupon, 50000.0, HOY))
                .isInstanceOf(CuponInvalidoException.class)
                .hasMessage("El cupón VERANO ya se usó todas las veces permitidas.");
    }

    @Test
    void sinLimiteDeUsos_nuncaSeAgota() {
        Cupon cupon = porcentaje(10).usosMaximos(null).usos(10_000).build();

        assertThat(cuponService.calcularDescuento(cupon, 1000.0, HOY)).isEqualTo(100.0);
    }

    @Test
    void sinLlegarAlMontoMinimo_diceCuantoFalta() {
        Cupon cupon = porcentaje(10).montoMinimo(50000.0).build();

        assertThatThrownBy(() -> cuponService.calcularDescuento(cupon, 42500.0, HOY))
                .isInstanceOf(CuponInvalidoException.class)
                .hasMessage("El cupón VERANO es para compras desde $ 50.000 en productos. Te faltan $ 7.500.");
    }

    @Test
    void justoEnElMontoMinimo_sirve() {
        Cupon cupon = porcentaje(10).montoMinimo(50000.0).build();

        assertThat(cuponService.calcularDescuento(cupon, 50000.0, HOY)).isEqualTo(5000.0);
    }

    @Test
    void dadoDeBaja_loRechaza() {
        Cupon cupon = porcentaje(10).activo(false).build();

        assertThatThrownBy(() -> cuponService.calcularDescuento(cupon, 50000.0, HOY))
                .isInstanceOf(CuponInvalidoException.class)
                .hasMessageContaining("ya no está disponible");
    }

    // ─── aplicar (al crear el pedido) ────────────────────────────────────────

    @Test
    void aplicar_normalizaElCodigoYSumaUnUso() {
        Cupon cupon = porcentaje(10).usosMaximos(2).usos(1).build();
        when(cuponRepository.findByCodigoParaActualizar("VERANO")).thenReturn(Optional.of(cupon));

        ICuponService.CuponAplicado aplicado = cuponService.aplicar("  verano ", 20000.0);

        assertThat(aplicado.descuento()).isEqualTo(2000.0);
        assertThat(cupon.getUsos()).isEqualTo(2);
        verify(cuponRepository).save(cupon);
    }

    @Test
    void aplicar_cuponInvalido_noSumaUso() {
        Cupon cupon = porcentaje(10).montoMinimo(50000.0).build();
        when(cuponRepository.findByCodigoParaActualizar("VERANO")).thenReturn(Optional.of(cupon));

        assertThatThrownBy(() -> cuponService.aplicar("VERANO", 100.0)).isInstanceOf(CuponInvalidoException.class);
        assertThat(cupon.getUsos()).isZero();
        verify(cuponRepository, never()).save(any());
    }

    @Test
    void aplicar_codigoInexistente_loRechaza() {
        when(cuponRepository.findByCodigoParaActualizar("NOEXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cuponService.aplicar("noexiste", 100.0))
                .isInstanceOf(CuponInvalidoException.class)
                .hasMessageContaining("No existe un cupón con el código NOEXISTE");
    }

    @Test
    void devolverUso_restaUnoSinBajarDeCero() {
        Cupon usado = porcentaje(10).usos(3).build();
        Cupon sinUsos = porcentaje(10).usos(0).build();

        cuponService.devolverUso(usado);
        cuponService.devolverUso(sinUsos);

        assertThat(usado.getUsos()).isEqualTo(2);
        assertThat(sinUsos.getUsos()).isZero();
    }

    // ─── panel ───────────────────────────────────────────────────────────────

    private CuponRequest request(String codigo, TipoCupon tipo, double valor, LocalDate desde, LocalDate hasta) {
        return new CuponRequest(codigo, tipo, valor, desde, hasta, null, null, null);
    }

    @Test
    void crear_guardaElCodigoEnMayusculasYSinMinimoQueda0() {
        when(cuponRepository.existsByCodigo("VERANO")).thenReturn(false);
        when(cuponRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CuponResponse creado = cuponService.crear(request("verano", TipoCupon.PORCENTAJE, 10, null, null));

        assertThat(creado.codigo()).isEqualTo("VERANO");
        assertThat(creado.montoMinimo()).isZero();
        assertThat(creado.activo()).isTrue();
        assertThat(creado.estado()).isEqualTo(CuponResponse.Estado.VIGENTE);
    }

    @Test
    void crear_codigoRepetido_lanzaDuplicado() {
        when(cuponRepository.existsByCodigo("VERANO")).thenReturn(true);

        assertThatThrownBy(() -> cuponService.crear(request("Verano", TipoCupon.PORCENTAJE, 10, null, null)))
                .isInstanceOf(DuplicadoException.class);
    }

    @Test
    void crear_porcentajeMayorA100_loRechaza() {
        assertThatThrownBy(() -> cuponService.crear(request("MUCHO", TipoCupon.PORCENTAJE, 120, null, null)))
                .isInstanceOf(EcommerceException.class)
                .hasMessageContaining("100%");
    }

    @Test
    void crear_fechasInvertidas_loRechaza() {
        assertThatThrownBy(() -> cuponService.crear(
                request("FECHAS", TipoCupon.MONTO_FIJO, 1000, LocalDate.now().plusDays(30), LocalDate.now().plusDays(10))))
                .isInstanceOf(EcommerceException.class)
                .hasMessageContaining("fecha de fin");
    }

    @Test
    void crear_fechaDeInicioPasada_laRechaza() {
        assertThatThrownBy(() -> cuponService.crear(
                request("VIEJO", TipoCupon.PORCENTAJE, 10, LocalDate.now().minusDays(1), null)))
                .isInstanceOf(EcommerceException.class)
                .hasMessageContaining("inicio no puede ser anterior a hoy");
    }

    @Test
    void crear_fechaDeFinPasada_laRechaza() {
        assertThatThrownBy(() -> cuponService.crear(
                request("VIEJO", TipoCupon.PORCENTAJE, 10, null, LocalDate.now().minusDays(1))))
                .isInstanceOf(EcommerceException.class)
                .hasMessageContaining("fin no puede ser anterior a hoy");
    }

    @Test
    void crear_empezandoHoy_sirve() {
        when(cuponRepository.existsByCodigo("HOY")).thenReturn(false);
        when(cuponRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CuponResponse creado = cuponService.crear(
                request("hoy", TipoCupon.PORCENTAJE, 10, LocalDate.now(), LocalDate.now()));

        assertThat(creado.codigo()).isEqualTo("HOY");
    }

    @Test
    void actualizar_conservaLaFechaPasadaQueYaTenia() {
        LocalDate empezo = LocalDate.now().minusDays(20);
        Cupon cupon = porcentaje(10).vigenteDesde(empezo).build();
        when(cuponRepository.findById(1L)).thenReturn(Optional.of(cupon));
        when(cuponRepository.findByCodigo(cupon.getCodigo())).thenReturn(Optional.of(cupon));
        when(cuponRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        cuponService.actualizar(1L, request(cupon.getCodigo(), TipoCupon.PORCENTAJE, 15, empezo, null));

        assertThat(cupon.getValor()).isEqualTo(15);
    }

    @Test
    void actualizar_cambiarAOtraFechaPasada_laRechaza() {
        Cupon cupon = porcentaje(10).vigenteDesde(LocalDate.now().minusDays(20)).build();
        when(cuponRepository.findById(1L)).thenReturn(Optional.of(cupon));
        when(cuponRepository.findByCodigo(cupon.getCodigo())).thenReturn(Optional.of(cupon));

        assertThatThrownBy(() -> cuponService.actualizar(1L,
                request(cupon.getCodigo(), TipoCupon.PORCENTAJE, 10, LocalDate.now().minusDays(5), null)))
                .isInstanceOf(EcommerceException.class)
                .hasMessageContaining("inicio no puede ser anterior a hoy");
    }

    @Test
    void actualizar_noDejaCambiarElCodigoDeUnCuponUsado() {
        Cupon usado = porcentaje(10).usos(4).build();
        when(cuponRepository.findById(1L)).thenReturn(Optional.of(usado));
        when(cuponRepository.findByCodigo("OTRO")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cuponService.actualizar(1L, request("otro", TipoCupon.PORCENTAJE, 10, null, null)))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void eliminar_esBajaLogica() {
        Cupon cupon = porcentaje(10).build();
        when(cuponRepository.findById(1L)).thenReturn(Optional.of(cupon));

        cuponService.eliminar(1L);

        assertThat(cupon.isActivo()).isFalse();
        verify(cuponRepository).save(cupon);
        verify(cuponRepository, never()).delete(any());
    }

    @Test
    void estado_reflejaComoEstaHoy() {
        assertThat(CuponService.estado(porcentaje(10).activo(false).build(), HOY)).isEqualTo(CuponResponse.Estado.INACTIVO);
        assertThat(CuponService.estado(porcentaje(10).vigenteHasta(HOY.minusDays(1)).build(), HOY)).isEqualTo(CuponResponse.Estado.VENCIDO);
        assertThat(CuponService.estado(porcentaje(10).usosMaximos(1).usos(1).build(), HOY)).isEqualTo(CuponResponse.Estado.AGOTADO);
        assertThat(CuponService.estado(porcentaje(10).vigenteDesde(HOY.plusDays(1)).build(), HOY)).isEqualTo(CuponResponse.Estado.PROGRAMADO);
        assertThat(CuponService.estado(porcentaje(10).build(), HOY)).isEqualTo(CuponResponse.Estado.VIGENTE);
    }
}
