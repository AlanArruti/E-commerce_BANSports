package com.BlandiArruti.E_commerce.enums;

/** Qué pasó con la plata al cancelar un pedido (RF04). */
public enum TipoReembolso {
    /** El pedido no estaba cobrado: no hay nada que devolver. */
    NO_CORRESPONDE,
    /** Se devolvió en la pasarela (Mercado Pago) al cancelar. */
    AUTOMATICO,
    /** Estaba cobrado fuera de una pasarela (lo marcó pagado el admin): la devolución la hace la tienda. */
    MANUAL
}
