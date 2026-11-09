package com.BlandiArruti.E_commerce.pago.pasarela;

import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Catálogo de pasarelas (Registry · Pure Fabrication). Spring le inyecta todas las clases que
 * implementan {@link PasarelaPago}, así que sumar una pasarela no requiere tocar esta clase.
 */
@Component
public class RegistroPasarelas {

    private final Map<String, PasarelaPago> porCodigo;

    public RegistroPasarelas(List<PasarelaPago> pasarelas) {
        this.porCodigo = pasarelas.stream().collect(Collectors.toMap(
                PasarelaPago::codigo, Function.identity(),
                (a, b) -> {
                    throw new IllegalStateException("Dos pasarelas con el mismo código: " + a.codigo());
                },
                LinkedHashMap::new));
    }

    public PasarelaPago obtener(String codigo) {
        PasarelaPago pasarela = codigo == null ? null : porCodigo.get(codigo);
        if (pasarela == null) {
            throw new EntidadNoEncontradaException("No existe la pasarela de pago '" + codigo + "'.");
        }
        return pasarela;
    }

    /** Las que tienen credenciales cargadas: son las que se le ofrecen al cliente. */
    public List<PasarelaPago> disponibles() {
        return porCodigo.values().stream().filter(PasarelaPago::estaDisponible).toList();
    }
}
