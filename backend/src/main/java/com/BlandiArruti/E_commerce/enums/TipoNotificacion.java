package com.BlandiArruti.E_commerce.enums;

/** De qué habla una notificación de la página. Define adónde lleva el enlace en el front. */
public enum TipoNotificacion {
    /** Cambio de estado de un pedido (pago, despacho, entrega...). La referencia es el id del pedido. */
    PEDIDO,
    /** Producto que entró en oferta. La referencia es el id del producto. */
    OFERTA
}
