package com.BlandiArruti.E_commerce.pedido.service;

import com.BlandiArruti.E_commerce.pedido.entity.Pedido;

/**
 * Lo que el pedido necesita del paquete pago/ para cancelarse cuando ya estaba cobrado (RF04).
 * La interfaz vive del lado del pedido (Inversión de Dependencias): así PedidoService no depende
 * de PagoService, que a su vez ya depende del pedido, y no se arma un ciclo.
 */
public interface IReembolsoPagos {

    /**
     * Devuelve en su pasarela cada cobro aprobado del pedido y los marca como reembolsados.
     * Lanza si alguna pasarela lo rechaza: el pedido no se tiene que cancelar.
     *
     * @return el total devuelto. 0 si el pedido no tenía cobros en una pasarela
     *         (por ejemplo, si el admin lo marcó pagado a mano).
     */
    double reembolsarCobros(Pedido pedido);
}
