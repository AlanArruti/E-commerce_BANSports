package com.BlandiArruti.E_commerce.pago.dto.response;

/**
 * @param pagoId  nuestro id del intento de pago (vuelve en la URL: /pago/resultado?pago=...)
 * @param urlPago página de la pasarela a la que se redirige al cliente
 */
public record InicioPagoResponse(Long pagoId, String urlPago) {}
