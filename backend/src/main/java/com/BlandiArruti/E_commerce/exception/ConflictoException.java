package com.BlandiArruti.E_commerce.exception;

public class ConflictoException extends EcommerceException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }

    public static ConflictoException categoriaConProductos(Long idCategoria) {
        return new ConflictoException(
            "No se puede eliminar la categoría con id " + idCategoria + " porque tiene productos asociados."
        );
    }
}
