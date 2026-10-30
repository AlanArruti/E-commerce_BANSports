package com.BlandiArruti.E_commerce.cliente.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Alta de una dirección elegida con el autocompletado de Google Maps.
 * La ciudad y la provincia salen del placeId (las consulta el back); calle, altura y
 * código postal llegan del formulario, precargados por Google y corregibles por el cliente.
 */
public record DireccionRequest(
        @NotBlank(message = "El nombre de la calle no puede estar vacío.")
        @Size(max = 100, message = "El nombre de la calle no puede superar los 100 caracteres.")
        String nombreCalle,

        @NotNull(message = "El número de calle es obligatorio.")
        @Positive(message = "El número de calle debe ser positivo.")
        Integer numeroCalle,

        @NotBlank(message = "El código postal no puede estar vacío.")
        @Size(max = 8, message = "El código postal no puede superar los 8 caracteres.")
        String codigoPostal,

        @NotBlank(message = "Elegí la dirección de la lista de sugerencias.")
        @Size(max = 300, message = "El identificador de la dirección no es válido.")
        String placeId,

        /** Id de la búsqueda en el front (el mismo de las sugerencias). Opcional. */
        @Size(max = 64, message = "La sesión de búsqueda no es válida.")
        String sesion
) {}
