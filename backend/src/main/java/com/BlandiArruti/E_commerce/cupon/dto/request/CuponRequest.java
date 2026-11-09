package com.BlandiArruti.E_commerce.cupon.dto.request;

import com.BlandiArruti.E_commerce.enums.TipoCupon;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

/** Alta y edición de un cupón (solo ADMIN). Las reglas que cruzan campos las valida el servicio. */
public record CuponRequest(
        @NotBlank(message = "Ingresá el código del cupón.")
        @Pattern(regexp = "^[A-Za-z0-9_-]{3,30}$",
                message = "El código tiene que tener de 3 a 30 letras, números, guiones o guiones bajos, sin espacios.")
        String codigo,

        @NotNull(message = "Elegí si el cupón es un porcentaje o un monto fijo.")
        TipoCupon tipo,

        @NotNull(message = "Ingresá el valor del descuento.")
        @Positive(message = "El descuento tiene que ser mayor a 0.")
        Double valor,

        LocalDate vigenteDesde,

        LocalDate vigenteHasta,

        @PositiveOrZero(message = "La compra mínima no puede ser negativa.")
        Double montoMinimo,

        @Positive(message = "Los usos máximos tienen que ser al menos 1 (vacío = sin límite).")
        Integer usosMaximos,

        /** Null = activo. */
        Boolean activo
) {}
