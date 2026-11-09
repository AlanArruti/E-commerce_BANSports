package com.BlandiArruti.E_commerce.pago.pasarela.dto;

/**
 * @param idExterno id del cobro en la pasarela (se guarda en Pago para consultarlo después)
 * @param urlPago   página de la pasarela a la que se redirige al cliente
 */
public record CobroIniciado(String idExterno, String urlPago) {}
