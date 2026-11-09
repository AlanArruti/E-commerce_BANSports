package com.BlandiArruti.E_commerce.notificacion.service;

import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.entity.Direccion;
import com.BlandiArruti.E_commerce.envio.entity.Envio;
import com.BlandiArruti.E_commerce.factura.entity.Factura;
import com.BlandiArruti.E_commerce.factura.pdf.FacturaPdfGenerator;
import com.BlandiArruti.E_commerce.notificacion.config.NotificacionProperties;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionService {

    private final JavaMailSender mailSender;
    private final NotificacionProperties notificacionProperties;
    private final FacturaPdfGenerator facturaPdfGenerator;

    public void notificarFacturaGenerada(Factura factura) {
        Pedido pedido = factura.getPedido();
        Cliente cliente = pedido.getCliente();

        String asunto = "Factura generada - Pedido #" + pedido.getId();
        String texto = """
                Hola %s,

                Generamos la factura tipo %s de tu pedido #%d por un total de $%.2f.
                Te la adjuntamos en PDF.

                ¡Gracias por tu compra!""".formatted(
                cliente.getNombre(), factura.getTipoFactura(), pedido.getId(), factura.getPrecioTotal());

        byte[] pdf = generarPdf(factura);
        if (pdf == null) {
            // Sin PDF igual avisamos: la factura existe y se puede descargar desde el pedido.
            enviar(cliente.getEmail(), asunto, texto.replace("\nTe la adjuntamos en PDF.", ""));
            return;
        }
        enviarConAdjunto(cliente.getEmail(), asunto, texto, facturaPdfGenerator.nombreArchivo(factura), pdf);
    }

    private byte[] generarPdf(Factura factura) {
        try {
            return facturaPdfGenerator.generar(factura);
        } catch (Exception e) {
            log.error("No se pudo generar el PDF de la factura del pedido {}: {}",
                    factura.getPedido().getId(), e.getMessage(), e);
            return null;
        }
    }

    public void notificarRegistroExitoso(Cliente cliente) {
        String asunto = "¡Bienvenido/a a E-commerce Blandi & Arruti!";
        String texto = """
                Hola %s,

                Tu cuenta se registró correctamente con el email %s.

                ¡Gracias por sumarte!""".formatted(cliente.getNombre(), cliente.getEmail());

        enviar(cliente.getEmail(), asunto, texto);
    }

    public void notificarRecuperacionPassword(Cliente cliente, String enlace, int minutosValidez) {
        String asunto = "Recuperá tu contraseña";
        String texto = """
                Hola %s,

                Pediste restablecer la contraseña de tu cuenta. Entrá a este enlace para elegir una nueva:

                %s

                El enlace vence en %d minutos y se puede usar una sola vez.
                Si no fuiste vos, ignorá este mail: tu contraseña no cambia.""".formatted(
                cliente.getNombre(), enlace, minutosValidez);

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

    private void enviarConAdjunto(String destinatario, String asunto, String texto,
                                  String nombreAdjunto, byte[] adjunto) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(notificacionProperties.getRemitente());
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(texto);
            helper.addAttachment(nombreAdjunto, new ByteArrayResource(adjunto), "application/pdf");
            mailSender.send(mensaje);
        } catch (Exception e) {
            log.error("No se pudo enviar el email a {}: {}", destinatario, e.getMessage());
        }
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
