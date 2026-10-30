package com.BlandiArruti.E_commerce.geo.service;

import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.geo.client.GooglePlacesClient;
import com.BlandiArruti.E_commerce.geo.client.dto.GoogleAutocompletarResponse;
import com.BlandiArruti.E_commerce.geo.client.dto.GoogleLugarResponse;
import com.BlandiArruti.E_commerce.geo.config.GoogleMapsProperties;
import com.BlandiArruti.E_commerce.geo.dto.response.LugarResponse;
import com.BlandiArruti.E_commerce.geo.dto.response.SugerenciaDireccionResponse;
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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Direcciones con Google Maps: sugerencias, detalle de un lugar y alta de la ciudad.
 *
 * No hay provincias ni ciudades precargadas: cuando un cliente guarda una dirección,
 * la provincia y la ciudad que devolvió Google se buscan en nuestras tablas y, si no
 * están, se crean. Así Direccion sigue apuntando a una Ciudad como antes.
 */
@Service
@RequiredArgsConstructor
public class LugarService {

    private static final int MINIMO_CARACTERES = 3;
    private static final int MAXIMO_CARACTERES = 200;
    /** Los placeId de Google son letras, números, guiones y guiones bajos. */
    private static final Pattern PLACE_ID_VALIDO = Pattern.compile("^[A-Za-z0-9_-]{1,300}$");
    private static final Pattern NUMERO = Pattern.compile("\\d+");

    // Largos de las columnas (ver entidades Pais, Provincia y Ciudad).
    private static final int LARGO_PAIS = 50;
    private static final int LARGO_PROVINCIA = 50;
    private static final int LARGO_CIUDAD = 100;

    private final GooglePlacesClient googlePlacesClient;
    private final GoogleMapsProperties properties;
    private final PaisRepository paisRepository;
    private final ProvinciaRepository provinciaRepository;
    private final CiudadRepository ciudadRepository;

    public List<SugerenciaDireccionResponse> sugerir(String texto, String sesion) {
        String busqueda = texto == null ? "" : texto.trim();
        if (busqueda.length() < MINIMO_CARACTERES) {
            return List.of();
        }
        if (busqueda.length() > MAXIMO_CARACTERES) {
            busqueda = busqueda.substring(0, MAXIMO_CARACTERES);
        }
        return googlePlacesClient.autocompletar(busqueda, sesion).stream()
                .map(LugarService::aSugerencia)
                .toList();
    }

    public LugarResponse obtener(String placeId, String sesion) {
        if (placeId == null || !PLACE_ID_VALIDO.matcher(placeId).matches()) {
            throw new EntidadNoEncontradaException("No encontramos esa dirección. Buscala de nuevo.");
        }
        return aLugar(googlePlacesClient.obtenerLugar(placeId, sesion));
    }

    /**
     * Busca (o crea) la ciudad del lugar elegido, para guardar la dirección.
     * Solo acepta direcciones del país configurado: hoy no se envía fuera de Argentina.
     */
    @Transactional
    public Ciudad resolverCiudad(String placeId, String sesion) {
        LugarResponse lugar = obtener(placeId, sesion);

        if (!properties.getRegion().equalsIgnoreCase(lugar.codigoPais())) {
            throw new EcommerceException("Por ahora solo enviamos dentro de Argentina.");
        }
        if (lugar.provincia() == null || lugar.ciudad() == null) {
            throw new EcommerceException(
                    "Esa dirección no tiene ciudad o provincia. Buscá la dirección completa, con calle y altura.");
        }

        Pais pais = paisRepository.findByNombreIgnoreCase(recortar(lugar.pais(), LARGO_PAIS))
                .orElseGet(() -> paisRepository.save(
                        Pais.builder().nombre(recortar(lugar.pais(), LARGO_PAIS)).build()));

        String nombreProvincia = recortar(lugar.provincia(), LARGO_PROVINCIA);
        Provincia provincia = provinciaRepository.findByNombreIgnoreCaseAndPaisId(nombreProvincia, pais.getId())
                .orElseGet(() -> provinciaRepository.save(
                        Provincia.builder().nombre(nombreProvincia).pais(pais).build()));

        String nombreCiudad = recortar(lugar.ciudad(), LARGO_CIUDAD);
        return ciudadRepository.findByNombreIgnoreCaseAndProvinciaId(nombreCiudad, provincia.getId())
                .orElseGet(() -> ciudadRepository.save(
                        Ciudad.builder().nombre(nombreCiudad).provincia(provincia).build()));
    }

    // ── Conversión de las respuestas de Google ─────────────────────────────

    private static SugerenciaDireccionResponse aSugerencia(GoogleAutocompletarResponse.PlacePrediction p) {
        GoogleAutocompletarResponse.FormatoEstructurado formato = p.structuredFormat();
        String principal = formato != null && formato.mainText() != null ? formato.mainText().text() : null;
        String secundario = formato != null && formato.secondaryText() != null ? formato.secondaryText().text() : null;
        if (principal == null && p.text() != null) {
            principal = p.text().text();
        }
        return new SugerenciaDireccionResponse(p.placeId(), principal, secundario);
    }

    static LugarResponse aLugar(GoogleLugarResponse lugar) {
        Optional<GoogleLugarResponse.Componente> pais = lugar.componente("country");
        return new LugarResponse(
                lugar.id(),
                lugar.formattedAddress(),
                texto(lugar, "route"),
                lugar.componente("street_number").map(c -> aNumero(c.longText())).orElse(null),
                texto(lugar, "postal_code"),
                // En la Ciudad de Buenos Aires Google a veces no manda "locality": se usa el
                // partido/comuna, el barrio o, en última instancia, la provincia.
                primero(lugar, "locality", "administrative_area_level_2", "sublocality_level_1",
                        "administrative_area_level_1"),
                texto(lugar, "administrative_area_level_1"),
                pais.map(GoogleLugarResponse.Componente::longText).orElse(null),
                pais.map(GoogleLugarResponse.Componente::shortText).orElse(null)
        );
    }

    private static String texto(GoogleLugarResponse lugar, String tipo) {
        return lugar.componente(tipo).map(GoogleLugarResponse.Componente::longText).orElse(null);
    }

    private static String primero(GoogleLugarResponse lugar, String... tipos) {
        return Stream.of(tipos)
                .map(tipo -> texto(lugar, tipo))
                .filter(valor -> valor != null && !valor.isBlank())
                .findFirst()
                .orElse(null);
    }

    /** "1234" → 1234; "1234 bis" → 1234; sin dígitos → null (lo completa el cliente). */
    private static Integer aNumero(String valor) {
        if (valor == null) {
            return null;
        }
        Matcher m = NUMERO.matcher(valor);
        if (!m.find()) {
            return null;
        }
        try {
            int numero = Integer.parseInt(m.group());
            return numero > 0 ? numero : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String recortar(String valor, int largo) {
        String limpio = valor.trim();
        return limpio.length() > largo ? limpio.substring(0, largo) : limpio;
    }
}
