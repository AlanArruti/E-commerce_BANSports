package com.BlandiArruti.E_commerce.enums;

/** Cómo descuenta un cupón sobre el subtotal de productos del pedido. */
public enum TipoCupon {
    /** Un porcentaje del subtotal (1 a 100). */
    PORCENTAJE,
    /** Un monto fijo en pesos, nunca más que el subtotal. */
    MONTO_FIJO
}
