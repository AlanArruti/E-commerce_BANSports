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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Localidades con Georef: buscador para el formulario y alta de la ciudad.
 *
 * No hay provincias ni ciudades precargadas: cuando un cliente guarda una dirección,
 * la provincia y la localidad que devuelve Georef se buscan en nuestras tablas y, si no
 * están, se crean. Así Direccion sigue apuntando a una Ciudad como antes.
 */
@Service
@RequiredArgsConstructor
public class LocalidadService {

    /** Georef solo cubre Argentina, que es también el único país al que enviamos. */
    public static final String PAIS = "Argentina";

    private static final int MINIMO_CARACTERES = 3;
    private static final int MAXIMO_CARACTERES = 100;
    private static final int MAXIMO_RESULTADOS = 10;
    /** Los ids de localidad censal de Georef son 8 dígitos. */
    private static final Pattern ID_VALIDO = Pattern.compile("^\\d{8}$");

    // Largos de las columnas (ver entidades Provincia y Ciudad).
    private static final int LARGO_PROVINCIA = 50;
    private static final int LARGO_CIUDAD = 100;

    private final GeorefClient georefClient;
    private final PaisRepository paisRepository;
    private final ProvinciaRepository provinciaRepository;
    private final CiudadRepository ciudadRepository;

    public List<LocalidadResponse> buscar(String texto) {
        String busqueda = texto == null ? "" : texto.trim();
        if (busqueda.length() < MINIMO_CARACTERES) {
            return List.of();
        }
        if (busqueda.length() > MAXIMO_CARACTERES) {
            busqueda = busqueda.substring(0, MAXIMO_CARACTERES);
        }
        return georefClient.buscarLocalidades(busqueda, MAXIMO_RESULTADOS).stream()
                .filter(LocalidadService::estaCompleta)
                .map(l -> new LocalidadResponse(l.id(), l.nombre(), nombre(l.departamento()), nombre(l.provincia())))
                .toList();
    }

    /** Busca (o crea) la ciudad de la localidad elegida, para guardar la dirección. */
    @Transactional
    public Ciudad resolverCiudad(String idLocalidad) {
        if (idLocalidad == null || !ID_VALIDO.matcher(idLocalidad).matches()) {
            throw noEncontrada();
        }
        Localidad localidad = georefClient.obtenerLocalidad(idLocalidad)
                .filter(LocalidadService::estaCompleta)
                .orElseThrow(LocalidadService::noEncontrada);

        Pais pais = paisRepository.findByNombreIgnoreCase(PAIS)
                .orElseGet(() -> paisRepository.save(Pais.builder().nombre(PAIS).build()));

        String nombreProvincia = recortar(localidad.provincia().nombre(), LARGO_PROVINCIA);
        Provincia provincia = provinciaRepository.findByNombreIgnoreCaseAndPaisId(nombreProvincia, pais.getId())
                .orElseGet(() -> provinciaRepository.save(
                        Provincia.builder().nombre(nombreProvincia).pais(pais).build()));

        String nombreCiudad = recortar(localidad.nombre(), LARGO_CIUDAD);
        return ciudadRepository.findByNombreIgnoreCaseAndProvinciaId(nombreCiudad, provincia.getId())
                .orElseGet(() -> ciudadRepository.save(
                        Ciudad.builder().nombre(nombreCiudad).provincia(provincia).build()));
    }

    private static boolean estaCompleta(Localidad l) {
        return l.id() != null && noVacio(l.nombre()) && l.provincia() != null && noVacio(l.provincia().nombre());
    }

    private static EntidadNoEncontradaException noEncontrada() {
        return new EntidadNoEncontradaException("No encontramos esa localidad. Buscala de nuevo.");
    }

    private static String nombre(Referencia referencia) {
        return referencia == null ? null : referencia.nombre();
    }

    private static boolean noVacio(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String recortar(String valor, int largo) {
        String limpio = valor.trim();
        return limpio.length() > largo ? limpio.substring(0, largo) : limpio;
    }
}
