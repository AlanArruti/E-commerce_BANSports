package com.BlandiArruti.E_commerce.pago.pasarela;

import com.BlandiArruti.E_commerce.pago.pasarela.dto.CobroIniciado;
import com.BlandiArruti.E_commerce.pago.pasarela.dto.ResultadoCobro;
import com.BlandiArruti.E_commerce.pago.pasarela.dto.SolicitudCobro;

import java.util.Map;
import java.util.Optional;

/**
 * Una pasarela de pago (MercadoPago, Stripe, MODO…). Es lo único que el resto del sistema
 * conoce de ellas (Variaciones Protegidas, Inversión de Dependencias): PagoService trabaja
 * contra esta interfaz y nunca contra una pasarela concreta.
 *
 * Para sumar una pasarela nueva alcanza con una clase que extienda {@link PasarelaPagoBase}
 * y tenga {@code @Component}: el {@link RegistroPasarelas} la encuentra solo (Abierto/Cerrado).
 */
public interface PasarelaPago {

    /** Identificador estable que viaja en la URL del webhook y se guarda en cada Pago. Ej: "mercadopago". */
    String codigo();

    /** Nombre para mostrarle al cliente. Ej: "Mercado Pago". */
    String nombre();

    /** false si faltan credenciales: el front no muestra esa opción. */
    boolean estaDisponible();

    /** Crea el cobro en la pasarela y devuelve la página a la que se redirige al cliente. */
    CobroIniciado iniciarCobro(SolicitudCobro solicitud);

    /** Consulta el estado real del cobro en la pasarela, traducido a nuestro EstadoPago. */
    ResultadoCobro consultarCobro(String idExterno);

    /**
     * Lee el aviso que manda la pasarela al webhook y devuelve el id del cobro que cambió.
     * Vacío si el aviso no es de un cobro (otros tópicos) o no se entiende.
     */
    Optional<String> leerNotificacion(Map<String, Object> cuerpo, Map<String, String> parametros);
}
