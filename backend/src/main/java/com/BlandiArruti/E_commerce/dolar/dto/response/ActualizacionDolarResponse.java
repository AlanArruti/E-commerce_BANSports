package com.BlandiArruti.E_commerce.dolar.dto.response;

/**
 * Resultado de consultar el dólar y, si corresponde, ajustar los precios.
 *
 * @param porcentajeAjuste   porcentaje en que se movieron los precios (0 si no se tocaron)
 * @param productosAjustados cuántos productos cambiaron de precio
 */
public record ActualizacionDolarResponse(
        CotizacionDolarResponse cotizacion,
        ResultadoAjuste resultado,
        Double porcentajeAjuste,
        int productosAjustados,
        String mensaje
) {

    public enum ResultadoAjuste {
        /** Primera cotización (o cambió el tipo de dólar): queda como referencia, los precios no se tocan. */
        BASE,
        /** Los precios se ajustaron por la variación del dólar. */
        AJUSTADO,
        /** El dólar se movió menos que el mínimo configurado: los precios quedan igual. */
        SIN_CAMBIOS,
        /** El dólar saltó más que el máximo configurado: hace falta que el admin lo confirme con forzar=true. */
        REQUIERE_CONFIRMACION
    }
}
