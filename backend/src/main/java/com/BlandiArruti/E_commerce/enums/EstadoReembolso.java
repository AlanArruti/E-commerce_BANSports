package com.BlandiArruti.E_commerce.enums;

/** RF05: reembolso de un pedido entregado que pide el cliente y resuelve el admin. */
public enum EstadoReembolso {
    /** El cliente lo pidió; falta que el admin lo revise. */
    SOLICITADO,
    /** El admin lo aprobó y se devolvió la plata (en Mercado Pago o a mano). */
    APROBADO,
    /** El admin lo rechazó, con un motivo. */
    RECHAZADO
}
