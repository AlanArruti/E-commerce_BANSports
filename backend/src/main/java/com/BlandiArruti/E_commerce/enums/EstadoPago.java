package com.BlandiArruti.E_commerce.enums;

/**
 * Estado de un intento de pago, igual para todas las pasarelas: cada pasarela traduce
 * sus propios estados a estos (ver PasarelaPago#consultarCobro).
 */
public enum EstadoPago {
    /** Se creó el cobro en la pasarela; el cliente todavía no pagó. */
    CREADO,
    /** La pasarela lo está procesando (por ejemplo, un pago en efectivo o en revisión). */
    PENDIENTE,
    /** Cobrado. Es el único estado que pasa el pedido a PAGADO. */
    APROBADO,
    /** Rechazado, vencido o cancelado. El cliente puede intentar de nuevo. */
    RECHAZADO;

    public boolean esFinal() {
        return this == APROBADO || this == RECHAZADO;
    }
}
