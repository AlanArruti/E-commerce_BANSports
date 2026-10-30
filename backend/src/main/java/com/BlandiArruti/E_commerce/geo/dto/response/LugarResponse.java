package com.BlandiArruti.E_commerce.geo.dto.response;

/**
 * Dirección elegida en el autocompletado, desarmada en partes para precargar el formulario.
 * numeroCalle y codigoPostal pueden venir en null (por ejemplo si se eligió solo una calle):
 * el cliente los completa a mano.
 */
public record LugarResponse(
        String placeId,
        String direccionCompleta,
        String nombreCalle,
        Integer numeroCalle,
        String codigoPostal,
        String ciudad,
        String provincia,
        String pais,
        String codigoPais
) {}
