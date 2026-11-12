package com.BlandiArruti.E_commerce.geo.util;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Códigos postales argentinos: formato y coherencia con la provincia de la localidad.
 *
 * Se aceptan los dos formatos en uso:
 * - viejo, 4 números (7600);
 * - CPA, letra de provincia + 4 números + 3 letras de la manzana (B7600ABC).
 *
 * Nadie publica gratis qué código corresponde a cada localidad (Georef no los trae), así que lo que
 * se controla es lo que sí es seguro:
 * - en un CPA, la primera letra es la provincia (la misma letra del ISO 3166-2:AR);
 * - los códigos 1000 a 1499 son todos de la Ciudad de Buenos Aires, y ella no usa otros.
 * Un 4 dígitos de otra provincia (ej. 5000 en Mar del Plata) no se puede detectar sin esa tabla.
 */
public final class CodigoPostal {

    /** Lo usa DireccionRequest (@Pattern), así el front recibe el mismo mensaje. */
    public static final String FORMATO = "^\\s*(\\d{4}|[A-Za-z]\\d{4}[A-Za-z]{3})\\s*$";
    public static final String MENSAJE_FORMATO =
            "El código postal tiene que tener 4 números (ej. 7600) o el formato CPA (ej. B7600ABC).";

    private static final Pattern PATRON = Pattern.compile(FORMATO);

    /** Código INDEC de la provincia (los 2 primeros dígitos del id de Georef) → letra del CPA. */
    private static final Map<String, Character> LETRA_POR_PROVINCIA = Map.ofEntries(
            Map.entry("02", 'C'), Map.entry("06", 'B'), Map.entry("10", 'K'), Map.entry("14", 'X'),
            Map.entry("18", 'W'), Map.entry("22", 'H'), Map.entry("26", 'U'), Map.entry("30", 'E'),
            Map.entry("34", 'P'), Map.entry("38", 'Y'), Map.entry("42", 'L'), Map.entry("46", 'F'),
            Map.entry("50", 'M'), Map.entry("54", 'N'), Map.entry("58", 'Q'), Map.entry("62", 'R'),
            Map.entry("66", 'A'), Map.entry("70", 'J'), Map.entry("74", 'D'), Map.entry("78", 'Z'),
            Map.entry("82", 'S'), Map.entry("86", 'G'), Map.entry("90", 'T'), Map.entry("94", 'V'));

    private static final Map<Character, String> PROVINCIA_POR_LETRA = Map.ofEntries(
            Map.entry('C', "Ciudad Autónoma de Buenos Aires"), Map.entry('B', "Buenos Aires"),
            Map.entry('K', "Catamarca"), Map.entry('X', "Córdoba"), Map.entry('W', "Corrientes"),
            Map.entry('H', "Chaco"), Map.entry('U', "Chubut"), Map.entry('E', "Entre Ríos"),
            Map.entry('P', "Formosa"), Map.entry('Y', "Jujuy"), Map.entry('L', "La Pampa"),
            Map.entry('F', "La Rioja"), Map.entry('M', "Mendoza"), Map.entry('N', "Misiones"),
            Map.entry('Q', "Neuquén"), Map.entry('R', "Río Negro"), Map.entry('A', "Salta"),
            Map.entry('J', "San Juan"), Map.entry('D', "San Luis"), Map.entry('Z', "Santa Cruz"),
            Map.entry('S', "Santa Fe"), Map.entry('G', "Santiago del Estero"), Map.entry('T', "Tucumán"),
            Map.entry('V', "Tierra del Fuego"));

    private static final String CABA = "02";

    private CodigoPostal() {
    }

    /** Sin espacios y en mayúsculas: "b7600abc " → "B7600ABC". */
    public static String normalizar(String codigo) {
        return codigo == null ? null : codigo.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean formatoValido(String codigo) {
        return codigo != null && PATRON.matcher(codigo).matches();
    }

    /**
     * Motivo por el que el código no puede ser de esa provincia, o vacío si no hay contradicción.
     *
     * @param codigo        ya normalizado y con formato válido
     * @param idProvincia   código INDEC de 2 dígitos (Georef)
     * @param nombreLugar   para el mensaje, ej. "Mar del Plata (Buenos Aires)"
     */
    public static Optional<String> contradiccionConProvincia(String codigo, String idProvincia, String nombreLugar) {
        Character letraEsperada = LETRA_POR_PROVINCIA.get(idProvincia);
        if (letraEsperada == null) {
            return Optional.empty();
        }
        if (Character.isLetter(codigo.charAt(0))) {
            char letra = codigo.charAt(0);
            if (letra != letraEsperada) {
                String provincia = PROVINCIA_POR_LETRA.getOrDefault(letra, "otra provincia");
                return Optional.of("El código postal " + codigo + " es de " + provincia + ", no de " + nombreLugar
                        + ". Revisá el código o la localidad.");
            }
            return Optional.empty();
        }
        int numero = Integer.parseInt(codigo);
        boolean esDeCaba = numero >= 1000 && numero <= 1499;
        if (esDeCaba && !CABA.equals(idProvincia)) {
            return Optional.of("El código postal " + codigo + " es de la Ciudad de Buenos Aires, no de " + nombreLugar
                    + ". Revisá el código o la localidad.");
        }
        if (!esDeCaba && CABA.equals(idProvincia)) {
            return Optional.of("Los códigos postales de la Ciudad de Buenos Aires van del 1000 al 1499 (ingresaste "
                    + codigo + "). Revisá el código o la localidad.");
        }
        return Optional.empty();
    }
}
