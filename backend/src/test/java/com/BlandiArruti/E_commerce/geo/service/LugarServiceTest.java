package com.BlandiArruti.E_commerce.geo.service;

import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.geo.client.GooglePlacesClient;
import com.BlandiArruti.E_commerce.geo.client.dto.GoogleLugarResponse;
import com.BlandiArruti.E_commerce.geo.client.dto.GoogleLugarResponse.Componente;
import com.BlandiArruti.E_commerce.geo.config.GoogleMapsProperties;
import com.BlandiArruti.E_commerce.geo.dto.response.LugarResponse;
import com.BlandiArruti.E_commerce.geo.entity.Ciudad;
import com.BlandiArruti.E_commerce.geo.entity.Pais;
import com.BlandiArruti.E_commerce.geo.entity.Provincia;
import com.BlandiArruti.E_commerce.geo.repository.CiudadRepository;
import com.BlandiArruti.E_commerce.geo.repository.PaisRepository;
import com.BlandiArruti.E_commerce.geo.repository.ProvinciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LugarServiceTest {

    @Mock GooglePlacesClient googlePlacesClient;
    @Mock PaisRepository paisRepository;
    @Mock ProvinciaRepository provinciaRepository;
    @Mock CiudadRepository ciudadRepository;
    @Spy GoogleMapsProperties properties = new GoogleMapsProperties();

    @InjectMocks LugarService lugarService;

    @BeforeEach
    void setUp() {
        properties.setRegion("AR");
    }

    private static GoogleLugarResponse lugarMarDelPlata() {
        return new GoogleLugarResponse("ChIJmdp", "Av. Independencia 1234, B7600 Mar del Plata, Buenos Aires, Argentina", List.of(
                new Componente("1234", "1234", List.of("street_number")),
                new Componente("Avenida Independencia", "Av. Independencia", List.of("route")),
                new Componente("Mar del Plata", "Mar del Plata", List.of("locality", "political")),
                new Componente("General Pueyrredón", "General Pueyrredón", List.of("administrative_area_level_2")),
                new Componente("Provincia de Buenos Aires", "Buenos Aires", List.of("administrative_area_level_1")),
                new Componente("Argentina", "AR", List.of("country", "political")),
                new Componente("B7600", "B7600", List.of("postal_code"))
        ));
    }

    @Test
    void sugerir_conMenosDeTresLetras_noLlamaAGoogle() {
        assertThat(lugarService.sugerir("  co ", "s")).isEmpty();
        verifyNoInteractions(googlePlacesClient);
    }

    @Test
    void obtener_desarmaLaDireccionEnPartes() {
        when(googlePlacesClient.obtenerLugar("ChIJmdp", "s")).thenReturn(lugarMarDelPlata());

        LugarResponse lugar = lugarService.obtener("ChIJmdp", "s");

        assertThat(lugar.nombreCalle()).isEqualTo("Avenida Independencia");
        assertThat(lugar.numeroCalle()).isEqualTo(1234);
        assertThat(lugar.codigoPostal()).isEqualTo("B7600");
        assertThat(lugar.ciudad()).isEqualTo("Mar del Plata");
        assertThat(lugar.provincia()).isEqualTo("Provincia de Buenos Aires");
        assertThat(lugar.codigoPais()).isEqualTo("AR");
    }

    @Test
    void obtener_sinLocalidad_usaElPartidoYSinAlturaDejaNull() {
        GoogleLugarResponse caba = new GoogleLugarResponse("ChIJcaba", "Av. Corrientes, CABA, Argentina", List.of(
                new Componente("Avenida Corrientes", "Av. Corrientes", List.of("route")),
                new Componente("Comuna 1", "Comuna 1", List.of("administrative_area_level_2")),
                new Componente("Ciudad Autónoma de Buenos Aires", "CABA", List.of("administrative_area_level_1")),
                new Componente("Argentina", "AR", List.of("country"))
        ));
        when(googlePlacesClient.obtenerLugar("ChIJcaba", null)).thenReturn(caba);

        LugarResponse lugar = lugarService.obtener("ChIJcaba", null);

        assertThat(lugar.ciudad()).isEqualTo("Comuna 1");
        assertThat(lugar.numeroCalle()).isNull();
        assertThat(lugar.codigoPostal()).isNull();
    }

    @Test
    void obtener_placeIdConCaracteresRaros_noLlamaAGoogle() {
        assertThatThrownBy(() -> lugarService.obtener("../admin", null))
                .isInstanceOf(EntidadNoEncontradaException.class);
        verifyNoInteractions(googlePlacesClient);
    }

    @Test
    void resolverCiudad_laPrimeraVez_creaPaisProvinciaYCiudad() {
        when(googlePlacesClient.obtenerLugar("ChIJmdp", "s")).thenReturn(lugarMarDelPlata());
        when(paisRepository.findByNombreIgnoreCase("Argentina")).thenReturn(Optional.empty());
        when(paisRepository.save(any(Pais.class))).thenAnswer(inv -> {
            Pais p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(provinciaRepository.findByNombreIgnoreCaseAndPaisId("Provincia de Buenos Aires", 1L)).thenReturn(Optional.empty());
        when(provinciaRepository.save(any(Provincia.class))).thenAnswer(inv -> {
            Provincia p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });
        when(ciudadRepository.findByNombreIgnoreCaseAndProvinciaId("Mar del Plata", 10L)).thenReturn(Optional.empty());
        when(ciudadRepository.save(any(Ciudad.class))).thenAnswer(inv -> inv.getArgument(0));

        Ciudad ciudad = lugarService.resolverCiudad("ChIJmdp", "s");

        assertThat(ciudad.getNombre()).isEqualTo("Mar del Plata");
        assertThat(ciudad.getProvincia().getNombre()).isEqualTo("Provincia de Buenos Aires");
        assertThat(ciudad.getProvincia().getPais().getNombre()).isEqualTo("Argentina");
    }

    @Test
    void resolverCiudad_siYaExiste_laReutiliza() {
        Pais argentina = Pais.builder().id(1L).nombre("Argentina").build();
        Provincia bsas = Provincia.builder().id(10L).nombre("Provincia de Buenos Aires").pais(argentina).build();
        Ciudad mdp = Ciudad.builder().id(100L).nombre("Mar del Plata").provincia(bsas).build();
        when(googlePlacesClient.obtenerLugar("ChIJmdp", "s")).thenReturn(lugarMarDelPlata());
        when(paisRepository.findByNombreIgnoreCase("Argentina")).thenReturn(Optional.of(argentina));
        when(provinciaRepository.findByNombreIgnoreCaseAndPaisId(anyString(), any())).thenReturn(Optional.of(bsas));
        when(ciudadRepository.findByNombreIgnoreCaseAndProvinciaId(anyString(), any())).thenReturn(Optional.of(mdp));

        assertThat(lugarService.resolverCiudad("ChIJmdp", "s").getId()).isEqualTo(100L);
        verify(ciudadRepository, never()).save(any());
    }

    @Test
    void resolverCiudad_fueraDeArgentina_seRechaza() {
        GoogleLugarResponse montevideo = new GoogleLugarResponse("ChIJmvd", "Montevideo, Uruguay", List.of(
                new Componente("Montevideo", "Montevideo", List.of("locality")),
                new Componente("Montevideo", "Montevideo", List.of("administrative_area_level_1")),
                new Componente("Uruguay", "UY", List.of("country"))
        ));
        when(googlePlacesClient.obtenerLugar("ChIJmvd", null)).thenReturn(montevideo);

        assertThatThrownBy(() -> lugarService.resolverCiudad("ChIJmvd", null))
                .isInstanceOf(EcommerceException.class)
                .hasMessageContaining("Argentina");
        verifyNoInteractions(paisRepository, provinciaRepository, ciudadRepository);
    }
}
