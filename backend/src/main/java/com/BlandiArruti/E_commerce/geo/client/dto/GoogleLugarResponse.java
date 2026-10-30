package com.BlandiArruti.E_commerce.geo.client.dto;

import java.util.List;
import java.util.Optional;

/** Respuesta de GET /places/{placeId} con el field mask id,formattedAddress,addressComponents. */
public record GoogleLugarResponse(String id, String formattedAddress, List<Componente> addressComponents) {

    /** Una parte de la dirección: calle, número, localidad, provincia, país, código postal... */
    public record Componente(String longText, String shortText, List<String> types) {}

    /** El primer componente que tenga el tipo pedido (por ejemplo "route" o "postal_code"). */
    public Optional<Componente> componente(String tipo) {
        if (addressComponents == null) {
            return Optional.empty();
        }
        return addressComponents.stream()
                .filter(c -> c.types() != null && c.types().contains(tipo))
                .findFirst();
    }
}
