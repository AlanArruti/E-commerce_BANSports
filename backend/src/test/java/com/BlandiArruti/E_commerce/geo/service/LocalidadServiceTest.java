package com.BlandiArruti.E_commerce.geo.service;

import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.geo.client.GeorefClient;
import com.BlandiArruti.E_commerce.geo.client.dto.GeorefLocalidadesResponse.Localidad;
import com.BlandiArruti.E_commerce.geo.client.dto.GeorefLocalidadesResponse.Referencia;
import com.BlandiArruti.E_commerce.geo.dto.response.LocalidadResponse;
import com.BlandiArruti.E_commerce.geo.entity.Ciudad;
import com.BlandiArruti.E_commerce.geo.entity.Pais;
import com.BlandiArruti.E_commerce.geo.entity.Provincia;
import com.BlandiArruti.E_commerce.geo.repository.CiudadRepository;
import com.BlandiArruti.E_commerce.geo.repository.PaisRepository;
import com.BlandiArruti.E_commerce.geo.repository.ProvinciaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
class LocalidadServiceTest {

    @Mock GeorefClient georefClient;
    @Mock PaisRepository paisRepository;
    @Mock ProvinciaRepository provinciaRepository;
    @Mock CiudadRepository ciudadRepository;

    @InjectMocks LocalidadService localidadService;

    private static Localidad marDelPlata() {
        return new Localidad("06357110", "Mar del Plata",
                new Referencia(null, "General Pueyrredón"), new Referencia(null, "Buenos Aires"));
    }

    @Test
    void buscar_conMenosDeTresLetras_noLlamaAGeoref() {
        assertThat(localidadService.buscar("  ma ")).isEmpty();
        verifyNoInteractions(georefClient);
    }

    @Test
    void buscar_devuelveLocalidadConPartidoYProvincia_yDescartaLasIncompletas() {
        Localidad sinProvincia = new Localidad("99999999", "Rara", null, null);
        when(georefClient.buscarLocalidades("mar del plata", 10)).thenReturn(List.of(marDelPlata(), sinProvincia));

        List<LocalidadResponse> resultado = localidadService.buscar(" mar del plata ");

        assertThat(resultado).containsExactly(
                new LocalidadResponse("06357110", "Mar del Plata", "General Pueyrredón", "Buenos Aires"));
    }

    @Test
    void resolverCiudad_idQueNoSonOchoDigitos_noLlamaAGeoref() {
        assertThatThrownBy(() -> localidadService.resolverCiudad("../admin"))
                .isInstanceOf(EntidadNoEncontradaException.class);
        verifyNoInteractions(georefClient);
    }

    @Test
    void resolverCiudad_idQueGeorefNoConoce_esNoEncontrada() {
        when(georefClient.obtenerLocalidad("12345678")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> localidadService.resolverCiudad("12345678"))
                .isInstanceOf(EntidadNoEncontradaException.class)
                .hasMessageContaining("localidad");
        verifyNoInteractions(paisRepository, provinciaRepository, ciudadRepository);
    }

    @Test
    void resolverCiudad_laPrimeraVez_creaPaisProvinciaYCiudad() {
        when(georefClient.obtenerLocalidad("06357110")).thenReturn(Optional.of(marDelPlata()));
        when(paisRepository.findByNombreIgnoreCase("Argentina")).thenReturn(Optional.empty());
        when(paisRepository.save(any(Pais.class))).thenAnswer(inv -> {
            Pais p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(provinciaRepository.findByNombreIgnoreCaseAndPaisId("Buenos Aires", 1L)).thenReturn(Optional.empty());
        when(provinciaRepository.save(any(Provincia.class))).thenAnswer(inv -> {
            Provincia p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });
        when(ciudadRepository.findByNombreIgnoreCaseAndProvinciaId("Mar del Plata", 10L)).thenReturn(Optional.empty());
        when(ciudadRepository.save(any(Ciudad.class))).thenAnswer(inv -> inv.getArgument(0));

        Ciudad ciudad = localidadService.resolverCiudad("06357110");

        assertThat(ciudad.getNombre()).isEqualTo("Mar del Plata");
        assertThat(ciudad.getProvincia().getNombre()).isEqualTo("Buenos Aires");
        assertThat(ciudad.getProvincia().getPais().getNombre()).isEqualTo("Argentina");
    }

    @Test
    void resolverCiudad_siYaExiste_laReutiliza() {
        Pais argentina = Pais.builder().id(1L).nombre("Argentina").build();
        Provincia bsas = Provincia.builder().id(10L).nombre("Buenos Aires").pais(argentina).build();
        Ciudad mdp = Ciudad.builder().id(100L).nombre("Mar del Plata").provincia(bsas).build();
        when(georefClient.obtenerLocalidad("06357110")).thenReturn(Optional.of(marDelPlata()));
        when(paisRepository.findByNombreIgnoreCase("Argentina")).thenReturn(Optional.of(argentina));
        when(provinciaRepository.findByNombreIgnoreCaseAndPaisId(anyString(), any())).thenReturn(Optional.of(bsas));
        when(ciudadRepository.findByNombreIgnoreCaseAndProvinciaId(anyString(), any())).thenReturn(Optional.of(mdp));

        assertThat(localidadService.resolverCiudad("06357110").getId()).isEqualTo(100L);
        verify(ciudadRepository, never()).save(any());
    }
}
