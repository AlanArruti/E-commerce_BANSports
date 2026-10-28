package com.BlandiArruti.E_commerce.exception;

public class EntidadNoEncontradaException extends EcommerceException {

    public EntidadNoEncontradaException(String mensaje) {
        super(mensaje);
    }

    // -- Clientes --
    public static EntidadNoEncontradaException cliente(Long id) {
        return new EntidadNoEncontradaException("Cliente con id " + id + " no encontrado.");
    }

    public static EntidadNoEncontradaException clientePorEmail(String email) {
        return new EntidadNoEncontradaException("Cliente con email '" + email + "' no encontrado.");
    }

    // -- Productos --
    public static EntidadNoEncontradaException producto(Long id) {
        return new EntidadNoEncontradaException("Producto con id " + id + " no encontrado.");
    }

    // -- Variantes --
    public static EntidadNoEncontradaException variante(Long id) {
        return new EntidadNoEncontradaException("Variante con id " + id + " no encontrada.");
    }

    // -- Categorías --
    public static EntidadNoEncontradaException categoria(Long id) {
        return new EntidadNoEncontradaException("Categoría con id " + id + " no encontrada.");
    }

    // -- Pedidos --
    public static EntidadNoEncontradaException pedido(Long id) {
        return new EntidadNoEncontradaException("Pedido con id " + id + " no encontrado.");
    }

    // -- Envíos --
    public static EntidadNoEncontradaException envio(Long id) {
        return new EntidadNoEncontradaException("Envío con id " + id + " no encontrado.");
    }

    // -- Facturas --
    public static EntidadNoEncontradaException factura(Long id) {
        return new EntidadNoEncontradaException("Factura con id " + id + " no encontrada.");
    }

    public static EntidadNoEncontradaException facturaPorPedido(Long idPedido) {
        return new EntidadNoEncontradaException("No existe factura para el pedido con id " + idPedido + ".");
    }

    // -- Direcciones --
    public static EntidadNoEncontradaException direccion(Long id) {
        return new EntidadNoEncontradaException("Dirección con id " + id + " no encontrada.");
    }

    // -- Administradores --
    public static EntidadNoEncontradaException administrador(Long id) {
        return new EntidadNoEncontradaException("Administrador con id " + id + " no encontrado.");
    }

    // -- Carritos --
    public static EntidadNoEncontradaException carrito(Long id) {
        return new EntidadNoEncontradaException("Carrito con id " + id + " no encontrado.");
    }

    // -- Items de carrito --
    public static EntidadNoEncontradaException itemCarrito(Long id) {
        return new EntidadNoEncontradaException("Ítem de carrito con id " + id + " no encontrado.");
    }
}