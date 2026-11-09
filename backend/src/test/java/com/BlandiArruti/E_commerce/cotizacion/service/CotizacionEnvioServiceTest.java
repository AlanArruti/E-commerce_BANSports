package com.BlandiArruti.E_commerce.cotizacion.service;

import com.BlandiArruti.E_commerce.administrador.entity.Administrador;
import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.entity.Direccion;
import com.BlandiArruti.E_commerce.cliente.repository.DireccionRepository;
import com.BlandiArruti.E_commerce.cotizacion.client.CorreoArgentinoClient;
import com.BlandiArruti.E_commerce.cotizacion.client.dto.CorreoArgentinoRatesResponse;
import com.BlandiArruti.E_commerce.cotizacion.config.CorreoArgentinoProperties;
import com.BlandiArruti.E_commerce.cotizacion.dto.response.CotizacionEnvioResponse;
import com.BlandiArruti.E_commerce.cotizacion.entity.TarifaEnvio;
import com.BlandiArruti.E_commerce.cotizacion.repository.TarifaEnvioRepository;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.geo.entity.Ciudad;
import com.BlandiArruti.E_commerce.geo.entity.Provincia;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CotizacionEnvioServiceTest {

    @Mock
    private DireccionRepository direccionRepository;

    @Mock
    private CorreoArgentinoClient correoArgentinoClient;

    @Mock
    private TarifaEnvioRepository tarifaEnvioRepository;

    private CotizacionEnvioService service;

    @BeforeEach
    void setUp() {
        CorreoArgentinoProperties properties = new CorreoArgentinoProperties();
        properties.setCustomerId("CUST123");
        properties.setCodigoPostalOrigen("1425");
        service = new CotizacionEnvioService(direccionRepository, correoArgentinoClient, properties, tarifaEnvioRepository);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void direccionInexistente_lanzaEntidadNoEncontrada() {
        when(direccionRepository.findById(99L)).thenReturn(Optional.empty());
        autenticarComo(admin());

        assertThatThrownBy(() -> service.cotizarPorDireccion(99L))
                .isInstanceOf(EntidadNoEncontradaException.class);
    }

    @Test
    void clienteQueNoEsDueno_lanzaAccessDenied() {
        Direccion direccion = Direccion.builder()
                .id(1L).codigoPostal("7600").cliente(Cliente.builder().id(2L).build())
                .build();
        when(direccionRepository.findById(1L)).thenReturn(Optional.of(direccion));
        autenticarComo(cliente(1L));

        assertThatThrownBy(() -> service.cotizarPorDireccion(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void direccionValidaDelPropioCliente_devuelveTarifasMapeadas() {
        Direccion direccion = Direccion.builder()
                .id(1L).codigoPostal("7600").cliente(Cliente.builder().id(1L).build())
                .build();
        when(direccionRepository.findById(1L)).thenReturn(Optional.of(direccion));
        autenticarComo(cliente(1L));

        CorreoArgentinoRatesResponse.Rate rate =
                new CorreoArgentinoRatesResponse.Rate("D", "CP", "Clásico", 4500.5, 3, 5);
        when(correoArgentinoClient.cotizar(any()))
                .thenReturn(new CorreoArgentinoRatesResponse("CUST123", "2026-12-31", List.of(rate)));

        List<CotizacionEnvioResponse> resultado = service.cotizarPorDireccion(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).servicio()).isEqualTo("Clásico");
        assertThat(resultado.get(0).precio()).isEqualTo(4500.5);
        assertThat(resultado.get(0).plazoMinimoDias()).isEqualTo(3);
        assertThat(resultado.get(0).plazoMaximoDias()).isEqualTo(5);
    }

    @Test
    void admin_puedeCotizarDireccionDeOtroCliente() {
        Direccion direccion = direccionConProvincia(1L, 2L, "7600", "Córdoba");
        when(direccionRepository.findById(1L)).thenReturn(Optional.of(direccion));
        autenticarComo(admin());

        when(correoArgentinoClient.cotizar(any()))
                .thenReturn(new CorreoArgentinoRatesResponse("CUST123", "2026-12-31", List.of()));
        when(tarifaEnvioRepository.findByProvinciaIgnoreCase("Córdoba"))
                .thenReturn(Optional.of(tarifa("Córdoba", 6000.0, 4, 5)));

        List<CotizacionEnvioResponse> resultado = service.cotizarPorDireccion(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).precio()).isEqualTo(6000.0);
    }

    @Test
    void direccionSinCodigoPostal_usaTablaPropiaSinLlamarAlExterno() {
        Direccion direccion = direccionConProvincia(1L, 1L, null, "Córdoba");
        when(direccionRepository.findById(1L)).thenReturn(Optional.of(direccion));
        autenticarComo(cliente(1L));
        when(tarifaEnvioRepository.findByProvinciaIgnoreCase("Córdoba"))
                .thenReturn(Optional.of(tarifa("Córdoba", 6000.0, 4, 5)));

        List<CotizacionEnvioResponse> resultado = service.cotizarPorDireccion(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).precio()).isEqualTo(6000.0);
        verify(correoArgentinoClient, never()).cotizar(any());
    }

    @Test
    void cotizacionExternaFalla_usaTablaPropia() {
        Direccion direccion = direccionConProvincia(1L, 1L, "7600", "Córdoba");
        when(direccionRepository.findById(1L)).thenReturn(Optional.of(direccion));
        autenticarComo(cliente(1L));
        when(correoArgentinoClient.cotizar(any())).thenThrow(new RuntimeException("sin conexión"));
        when(tarifaEnvioRepository.findByProvinciaIgnoreCase("Córdoba"))
                .thenReturn(Optional.of(tarifa("Córdoba", 6000.0, 4, 5)));

        List<CotizacionEnvioResponse> resultado = service.cotizarPorDireccion(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).precio()).isEqualTo(6000.0);
        assertThat(resultado.get(0).plazoMinimoDias()).isEqualTo(4);
        assertThat(resultado.get(0).plazoMaximoDias()).isEqualTo(5);
    }

    @Test
    void sinTarifaParaLaProvincia_usaTarifaDefault() {
        Direccion direccion = direccionConProvincia(1L, 1L, null, "Tierra del Fuego");
        when(direccionRepository.findById(1L)).thenReturn(Optional.of(direccion));
        autenticarComo(cliente(1L));
        when(tarifaEnvioRepository.findByProvinciaIgnoreCase("Tierra del Fuego"))
                .thenReturn(Optional.empty());
        when(tarifaEnvioRepository.findByProvinciaIsNull())
                .thenReturn(Optional.of(tarifa(null, 6000.0, 3, 5)));

        List<CotizacionEnvioResponse> resultado = service.cotizarPorDireccion(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).precio()).isEqualTo(6000.0);
        assertThat(resultado.get(0).plazoMinimoDias()).isEqualTo(3);
    }

    private Direccion direccionConProvincia(Long id, Long idCliente, String codigoPostal, String nombreProvincia) {
        Provincia provincia = Provincia.builder().nombre(nombreProvincia).build();
        Ciudad ciudad = Ciudad.builder().provincia(provincia).build();
        return Direccion.builder()
                .id(id).codigoPostal(codigoPostal).cliente(Cliente.builder().id(idCliente).build())
                .ciudad(ciudad)
                .build();
    }

    private TarifaEnvio tarifa(String provincia, Double precio, Integer plazoMinimo, Integer plazoMaximo) {
        return TarifaEnvio.builder()
                .provincia(provincia)
                .precio(precio)
                .plazoMinimoDias(plazoMinimo)
                .plazoMaximoDias(plazoMaximo)
                .build();
    }

    private void autenticarComo(UsuarioDetails usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
    }

    private UsuarioDetails admin() {
        return UsuarioDetails.fromAdmin(Administrador.builder().id(99L).username("admin").contrasenia("x").build());
    }

    private UsuarioDetails cliente(Long id) {
        return UsuarioDetails.fromCliente(Cliente.builder().id(id).email("c@mail.com").contrasenia("x").build());
    }
}
