package com.BlandiArruti.E_commerce.notificacion.service;

import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.entity.Direccion;
import com.BlandiArruti.E_commerce.enums.EstadoEnvio;
import com.BlandiArruti.E_commerce.envio.entity.Envio;
import com.BlandiArruti.E_commerce.factura.entity.Factura;
import com.BlandiArruti.E_commerce.factura.pdf.FacturaPdfGenerator;
import com.BlandiArruti.E_commerce.notificacion.config.NotificacionProperties;
import com.BlandiArruti.E_commerce.pedido.entity.ItemPedido;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mails al cliente. Cada uno va en HTML (plantillas Thymeleaf de resources/templates/mail,
 * con el diseño del sitio) y en texto plano, que es lo que muestran los clientes de correo
 * que no leen HTML y lo que miran los filtros de spam.
 */
@Service
@Slf4j
public class NotificacionService {

    private static final Locale ES_AR = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JavaMailSender mailSender;
    private final NotificacionProperties notificacionProperties;
    private final ITemplateEngine templateEngine;
    private final FacturaPdfGenerator facturaPdfGenerator;
    private final String frontUrl;

    public NotificacionService(JavaMailSender mailSender,
                               NotificacionProperties notificacionProperties,
                               ITemplateEngine templateEngine,
                               FacturaPdfGenerator facturaPdfGenerator,
                               @Value("${app.front-url:http://localhost:4200}") String frontUrl) {
        this.mailSender = mailSender;
        this.notificacionProperties = notificacionProperties;
        this.templateEngine = templateEngine;
        this.facturaPdfGenerator = facturaPdfGenerator;
        this.frontUrl = frontUrl.endsWith("/") ? frontUrl.substring(0, frontUrl.length() - 1) : frontUrl;
    }

    /** Una línea del detalle del pedido, ya formateada para la plantilla. */
    public record LineaMail(String nombre, String detalle, int cantidad, String importe) {
    }

    /** Una etapa del seguimiento del envío. */
    public record PasoEnvio(String etiqueta, boolean hecho, boolean actual) {
    }

    /** Archivo que viaja adjunto al mail (la factura en PDF). */
    private record Adjunto(String nombre, byte[] contenido, String tipo) {
    }

    public void notificarFacturaGenerada(Factura factura) {
        Pedido pedido = factura.getPedido();
        Cliente cliente = pedido.getCliente();

        List<LineaMail> lineas = new ArrayList<>();
        double subtotal = 0;
        for (ItemPedido item : pedido.getItems() != null ? pedido.getItems() : List.<ItemPedido>of()) {
            double importe = item.getPrecioProducto() != null ? item.getPrecioProducto() : 0.0;
            subtotal += importe;
            lineas.add(new LineaMail(
                    item.getProducto() != null ? item.getProducto().getNombre() : "Producto",
                    detalleVariante(item),
                    item.getCantidad(),
                    pesos(importe)));
        }
        double descuento = pedido.getDescuentoCupon() != null ? pedido.getDescuentoCupon() : 0.0;
        double costoEnvio = pedido.getCostoEnvio() != null ? pedido.getCostoEnvio() : 0.0;
        boolean conCupon = descuento > 0 && pedido.getCupon() != null;
        String direccion = pedido.getDireccionEnvio() != null ? direccion(pedido.getDireccionEnvio()) : null;

        String asunto = "Pago confirmado - Pedido #" + pedido.getId();
        Context contexto = contexto(asunto, "Acreditamos el pago de tu pedido #" + pedido.getId()
                + " por " + pesos(factura.getPrecioTotal()) + ".");
        contexto.setVariable("nombre", cliente.getNombre());
        contexto.setVariable("idPedido", pedido.getId());
        contexto.setVariable("tipoFactura", factura.getTipoFactura());
        contexto.setVariable("lineas", lineas);
        contexto.setVariable("subtotal", pesos(subtotal));
        contexto.setVariable("descuento", conCupon ? pesos(descuento) : null);
        contexto.setVariable("codigoCupon", conCupon ? pedido.getCupon().getCodigo() : null);
        contexto.setVariable("envio", costoEnvio > 0 ? pesos(costoEnvio) : "Gratis");
        contexto.setVariable("total", pesos(factura.getPrecioTotal()));
        contexto.setVariable("direccion", direccion);

        // La factura va adjunta en PDF. Si no se pudo generar, el mail sale igual (se descarga desde el pedido).
        Adjunto facturaPdf = facturaEnPdf(factura);
        contexto.setVariable("facturaAdjunta", facturaPdf != null);

        String texto = """
                Hola %s,

                Acreditamos el pago de tu pedido #%d y generamos la factura tipo %s por un total de %s.
                %s%s
                ¡Gracias por tu compra!

                Ver mis pedidos: %s/perfil""".formatted(
                cliente.getNombre(), pedido.getId(), factura.getTipoFactura(), pesos(factura.getPrecioTotal()),
                facturaPdf != null ? "Te adjuntamos la factura en PDF.\n" : "",
                direccion != null ? "Se envía a: " + direccion + "\n" : "",
                frontUrl);

        enviar(cliente.getEmail(), asunto, "mail/pago-confirmado", contexto, texto, facturaPdf);
    }

    private Adjunto facturaEnPdf(Factura factura) {
        try {
            return new Adjunto(facturaPdfGenerator.nombreArchivo(factura), facturaPdfGenerator.generar(factura),
                    "application/pdf");
        } catch (Exception e) {
            log.error("No se pudo generar el PDF de la factura del pedido {}: {}",
                    factura.getPedido().getId(), e.getMessage(), e);
            return null;
        }
    }

    public void notificarRegistroExitoso(Cliente cliente) {
        String asunto = "¡Bienvenido/a a BAN Sports!";
        Context contexto = contexto(asunto, "Tu cuenta ya está lista. Mirá los productos y ofertas.");
        contexto.setVariable("nombre", cliente.getNombre());
        contexto.setVariable("email", cliente.getEmail());

        String texto = """
                Hola %s,

                Tu cuenta se registró correctamente con el email %s.

                ¡Gracias por sumarte! Mirá los productos en %s/productos""".formatted(
                cliente.getNombre(), cliente.getEmail(), frontUrl);

        enviar(cliente.getEmail(), asunto, "mail/bienvenida", contexto, texto);
    }

    public void notificarRecuperacionPassword(Cliente cliente, String enlace, int minutosValidez) {
        String asunto = "Recuperá tu contraseña";
        Context contexto = contexto(asunto, "Elegí una nueva contraseña. El enlace vence en "
                + minutosValidez + " minutos.");
        contexto.setVariable("nombre", cliente.getNombre());
        contexto.setVariable("enlace", enlace);
        contexto.setVariable("minutosValidez", minutosValidez);

        String texto = """
                Hola %s,

                Pediste restablecer la contraseña de tu cuenta. Entrá a este enlace para elegir una nueva:

                %s

                El enlace vence en %d minutos y se puede usar una sola vez.
                Si no fuiste vos, ignorá este mail: tu contraseña no cambia.""".formatted(
                cliente.getNombre(), enlace, minutosValidez);

        enviar(cliente.getEmail(), asunto, "mail/recuperar-password", contexto, texto);
    }

    public void notificarEnvioActualizado(Envio envio) {
        Pedido pedido = envio.getPedido();
        Cliente cliente = pedido.getCliente();
        EstadoEnvio estado = envio.getEstado();
        String direccion = direccion(envio.getDireccion());

        String[] titulo = switch (estado) {
            case DESPACHADO -> new String[]{"Tu pedido", "ya salió"};
            case EN_CAMINO -> new String[]{"Tu pedido está", "en camino"};
            case ENTREGADO -> new String[]{"Tu pedido", "llegó"};
        };
        String mensaje = switch (estado) {
            case DESPACHADO -> "preparamos tu pedido y ya lo despachamos.";
            case EN_CAMINO -> "tu pedido ya está viajando hacia la dirección de entrega.";
            case ENTREGADO -> "tu pedido fue entregado. ¡Que lo disfrutes!";
        };

        List<PasoEnvio> pasos = new ArrayList<>();
        for (EstadoEnvio paso : EstadoEnvio.values()) {
            pasos.add(new PasoEnvio(etiqueta(paso), paso.ordinal() <= estado.ordinal(), paso == estado));
        }

        String asunto = "Tu pedido #" + pedido.getId() + ": " + etiqueta(estado).toLowerCase(ES_AR);
        Context contexto = contexto(asunto, titulo[0] + " " + titulo[1] + ". Pedido #" + pedido.getId() + ".");
        contexto.setVariable("nombre", cliente.getNombre());
        contexto.setVariable("idPedido", pedido.getId());
        contexto.setVariable("tituloInicio", titulo[0]);
        contexto.setVariable("tituloAcento", titulo[1]);
        contexto.setVariable("mensaje", mensaje);
        contexto.setVariable("pasos", pasos);
        contexto.setVariable("direccion", direccion);
        contexto.setVariable("fechaSalida", fecha(envio.getFechaSalida()));
        contexto.setVariable("fechaLlegada", fecha(envio.getFechaLlegada()));
        contexto.setVariable("entregado", estado == EstadoEnvio.ENTREGADO);

        String texto = """
                Hola %s,

                El envío de tu pedido #%d cambió de estado a: %s.

                Dirección de entrega: %s.

                Ver mis pedidos: %s/perfil""".formatted(
                cliente.getNombre(), pedido.getId(), etiqueta(estado), direccion, frontUrl);

        enviar(cliente.getEmail(), asunto, "mail/envio", contexto, texto);
    }

    private Context contexto(String asunto, String preheader) {
        Context contexto = new Context(ES_AR);
        contexto.setVariable("asunto", asunto);
        contexto.setVariable("preheader", preheader);
        contexto.setVariable("frontUrl", frontUrl);
        contexto.setVariable("anio", Year.now().getValue());
        return contexto;
    }

    private void enviar(String destinatario, String asunto, String plantilla, Context contexto, String texto) {
        enviar(destinatario, asunto, plantilla, contexto, texto, null);
    }

    private void enviar(String destinatario, String asunto, String plantilla, Context contexto, String texto,
                        Adjunto adjunto) {
        try {
            String html = templateEngine.process(plantilla, contexto);
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(notificacionProperties.getRemitente());
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(texto, html);
            if (adjunto != null) {
                helper.addAttachment(adjunto.nombre(), new ByteArrayResource(adjunto.contenido()), adjunto.tipo());
            }
            mailSender.send(mensaje);
        } catch (Exception e) {
            // El mail es un aviso: si falla no tiene que romper el registro, el pago ni el envío.
            log.error("No se pudo enviar el email a {}: {}", destinatario, e.getMessage());
        }
    }

    private static String etiqueta(EstadoEnvio estado) {
        return switch (estado) {
            case DESPACHADO -> "Despachado";
            case EN_CAMINO -> "En camino";
            case ENTREGADO -> "Entregado";
        };
    }

    /** "Chocolate · 1 kg": los valores de los atributos de la variante, si tiene. */
    private static String detalleVariante(ItemPedido item) {
        if (item.getVariante() == null) {
            return null;
        }
        Map<String, String> atributos = item.getVariante().getAtributos();
        if (atributos == null || atributos.isEmpty()) {
            return null;
        }
        return atributos.values().stream().collect(Collectors.joining(" · "));
    }

    private static String direccion(Direccion direccion) {
        String ciudad = direccion.getCiudad() != null ? ", " + direccion.getCiudad().getNombre() : "";
        String cp = direccion.getCodigoPostal() != null ? " (CP " + direccion.getCodigoPostal() + ")" : "";
        return direccion.getNombreCalle() + " " + direccion.getNumeroCalle() + ciudad + cp;
    }

    private static String fecha(LocalDate fecha) {
        return fecha != null ? fecha.format(FECHA) : null;
    }

    /** $ 28.900 o $ 1.234,50: el mismo formato que usa el front. */
    private static String pesos(Double monto) {
        double valor = monto != null ? monto : 0.0;
        boolean entero = Math.abs(valor - Math.rint(valor)) < 0.005;
        return "$ " + String.format(ES_AR, entero ? "%,.0f" : "%,.2f", valor);
    }
}
