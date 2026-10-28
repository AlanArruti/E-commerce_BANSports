package com.BlandiArruti.E_commerce.notificacion.service;

import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.entity.Direccion;
import com.BlandiArruti.E_commerce.envio.entity.Envio;
import com.BlandiArruti.E_commerce.factura.entity.Factura;
import com.BlandiArruti.E_commerce.notificacion.config.NotificacionProperties;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionService {

    private final JavaMailSender mailSender;
    private final NotificacionProperties notificacionProperties;

    public void notificarFacturaGenerada(Factura factura) {
        Pedido pedido = factura.getPedido();
        Cliente cliente = pedido.getCliente();

        String asunto = "Factura generada - Pedido #" + pedido.getId();
        String texto = """
                Hola %s,

                Generamos la factura tipo %s de tu pedido #%d por un total de $%.2f.

                ¡Gracias por tu compra!""".formatted(
                cliente.getNombre(), factura.getTipoFactura(), pedido.getId(), factura.getPrecioTotal());

        enviar(cliente.getEmail(), asunto, texto);
    }

    public void notificarRegistroExitoso(Cliente cliente) {
        String asunto = "¡Bienvenido/a a E-commerce Blandi & Arruti!";
        String texto = """
                Hola %s,

                Tu cuenta se registró correctamente con el email %s.

                ¡Gracias por sumarte!""".formatted(cliente.getNombre(), cliente.getEmail());

        enviar(cliente.getEmail(), asunto, texto);
    }

    public void notificarEnvioActualizado(Envio envio) {
        Pedido pedido = envio.getPedido();
        Cliente cliente = pedido.getCliente();
        Direccion direccion = envio.getDireccion();

        String asunto = "Actualización de envío - Pedido #" + pedido.getId();
        String texto = """
                Hola %s,

                El envío de tu pedido #%d cambió de estado a: %s.

                Dirección de entrega: %s %d, %s (CP %s).""".formatted(
                cliente.getNombre(), pedido.getId(), envio.getEstado(),
                direccion.getNombreCalle(), direccion.getNumeroCalle(),
                direccion.getCiudad().getNombre(), direccion.getCodigoPostal());

        enviar(cliente.getEmail(), asunto, texto);
    }

    private void enviar(String destinatario, String asunto, String texto) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(notificacionProperties.getRemitente());
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(texto);
            mailSender.send(mensaje);
        } catch (Exception e) {
            log.error("No se pudo enviar el email a {}: {}", destinatario, e.getMessage());
        }
    }
}
