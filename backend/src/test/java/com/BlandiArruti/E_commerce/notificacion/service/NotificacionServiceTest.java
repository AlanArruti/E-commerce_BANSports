package com.BlandiArruti.E_commerce.notificacion.service;

import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.entity.Direccion;
import com.BlandiArruti.E_commerce.cupon.entity.Cupon;
import com.BlandiArruti.E_commerce.enums.EstadoEnvio;
import com.BlandiArruti.E_commerce.enums.TipoFactura;
import com.BlandiArruti.E_commerce.envio.entity.Envio;
import com.BlandiArruti.E_commerce.factura.entity.Factura;
import com.BlandiArruti.E_commerce.factura.pdf.FacturaPdfGenerator;
import com.BlandiArruti.E_commerce.geo.entity.Ciudad;
import com.BlandiArruti.E_commerce.notificacion.config.NotificacionProperties;
import com.BlandiArruti.E_commerce.pedido.entity.ItemPedido;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import com.BlandiArruti.E_commerce.producto.entity.Variante;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Usa las plantillas reales de resources/templates/mail: si una plantilla se rompe, falla acá
 * y no en producción (donde el error solo quedaría en el log, porque el mail no corta el flujo).
 */
@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private NotificacionService service;
    private NotificacionProperties properties;
    private SpringTemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        NotificacionProperties properties = new NotificacionProperties();
        properties.setRemitente("no-reply@ecommerce-blandi-arruti.com");

        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        service = new NotificacionService(mailSender, properties, templateEngine, new FacturaPdfGenerator(),
                "http://localhost:4200/");
        this.properties = properties;
        this.templateEngine = templateEngine;
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    /** El mail que se mandó, con sus dos versiones (texto plano y HTML). */
    private record MailEnviado(MimeMessage mensaje, String texto, String html) {
    }

    private MailEnviado mailEnviado() throws Exception {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mensaje = captor.getValue();
        // JavaMailSender lo hace al mandar; sin esto las partes todavía no tienen su Content-Type.
        mensaje.saveChanges();
        List<String> textos = new ArrayList<>();
        List<String> htmls = new ArrayList<>();
        juntarPartes(mensaje, textos, htmls);
        assertThat(textos).as("versión en texto plano").hasSize(1);
        assertThat(htmls).as("versión HTML").hasSize(1);
        return new MailEnviado(mensaje, textos.getFirst(), htmls.getFirst());
    }

    /** El adjunto del mail (la factura en PDF), o null si no tiene. */
    private static Part adjunto(MimeMessage mensaje) throws Exception {
        Multipart raiz = (Multipart) mensaje.getContent();
        for (int i = 0; i < raiz.getCount(); i++) {
            BodyPart parte = raiz.getBodyPart(i);
            if (Part.ATTACHMENT.equalsIgnoreCase(parte.getDisposition())) {
                return parte;
            }
        }
        return null;
    }

    private static void juntarPartes(Part parte, List<String> textos, List<String> htmls) throws Exception {
        Object contenido = parte.getContent();
        if (contenido instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart hija = multipart.getBodyPart(i);
                juntarPartes(hija, textos, htmls);
            }
        } else if (parte.isMimeType("text/html")) {
            htmls.add((String) contenido);
        } else if (parte.isMimeType("text/plain")) {
            textos.add((String) contenido);
        }
    }

    private static Direccion direccion() {
        return Direccion.builder()
                .nombreCalle("Calle 13")
                .numeroCalle(567)
                .codigoPostal("1900")
                .ciudad(Ciudad.builder().nombre("La Plata").build())
                .build();
    }

    @Test
    void notificarFacturaGenerada_mandaElDetalleDelPedidoConElDisenioDelSitio() throws Exception {
        Cliente cliente = Cliente.builder().nombre("Juan").email("juan@mail.com").build();
        Map<String, String> atributos = new LinkedHashMap<>();
        atributos.put("sabor", "Chocolate");
        atributos.put("peso", "1 kg");
        Pedido pedido = Pedido.builder()
                .id(10L)
                .cliente(cliente)
                .direccionEnvio(direccion())
                .costoEnvio(4500.0)
                .descuentoCupon(6000.0)
                .cupon(Cupon.builder().codigo("VERANO").build())
                .build();
        pedido.getItems().add(ItemPedido.builder()
                .producto(Producto.builder().nombre("Whey Protein").build())
                .variante(Variante.builder().atributos(atributos).build())
                .cantidad(2)
                .precioProducto(60000.0)
                .build());
        Factura factura = Factura.builder()
                .id(3L)
                .pedido(pedido)
                .tipoFactura(TipoFactura.B)
                .precioTotal(58500.0)
                .build();

        service.notificarFacturaGenerada(factura);

        MailEnviado mail = mailEnviado();
        assertThat(mail.mensaje().getFrom()).containsExactly(new InternetAddress("no-reply@ecommerce-blandi-arruti.com"));
        assertThat(mail.mensaje().getAllRecipients()).containsExactly(new InternetAddress("juan@mail.com"));
        assertThat(mail.mensaje().getSubject()).isEqualTo("Pago confirmado - Pedido #10");
        assertThat(mail.html())
                .contains("Juan", "factura B", "Pedido #10")
                .contains("2 ×", "Whey Protein", "Chocolate · 1 kg", "$ 60.000")
                .contains("VERANO", "− $ 6.000", "$ 4.500", "$ 58.500")
                .contains("Calle 13 567, La Plata (CP 1900)")
                .contains("http://localhost:4200/perfil")
                // La paleta del sitio: fondo, tarjeta y verde de acento.
                .contains("#0a0d0b", "#121714", "#2be37a");
        assertThat(mail.texto()).contains("Juan", "#10", "B", "$ 58.500", "adjuntamos la factura en PDF");

        Part pdf = adjunto(mail.mensaje());
        assertThat(pdf).as("factura adjunta").isNotNull();
        assertThat(pdf.getFileName()).isEqualTo("factura-B-0001-00000003.pdf");
        assertThat(pdf.getContentType()).startsWith("application/pdf");
        assertThat(pdf.getInputStream().readNBytes(5)).isEqualTo("%PDF-".getBytes());
        assertThat(mail.html()).contains("Te la adjuntamos en PDF.");
    }

    @Test
    void notificarFacturaGenerada_siFallaElPdf_mandaElMailSinAdjunto() throws Exception {
        FacturaPdfGenerator generadorRoto = mock(FacturaPdfGenerator.class);
        when(generadorRoto.generar(any())).thenThrow(new IllegalStateException("fuente rota"));
        NotificacionService conPdfRoto = new NotificacionService(mailSender, properties, templateEngine,
                generadorRoto, "http://localhost:4200/");
        Cliente cliente = Cliente.builder().nombre("Juan").email("juan@mail.com").build();
        Pedido pedido = Pedido.builder().id(12L).cliente(cliente).build();
        Factura factura = Factura.builder().id(4L).pedido(pedido).tipoFactura(TipoFactura.B).precioTotal(100.0).build();

        conPdfRoto.notificarFacturaGenerada(factura);

        MailEnviado mail = mailEnviado();
        assertThat(adjunto(mail.mensaje())).isNull();
        assertThat(mail.html()).doesNotContain("adjuntamos");
        assertThat(mail.texto()).doesNotContain("adjuntamos");
    }

    @Test
    void notificarFacturaGenerada_sinCuponNiEnvio_noMuestraElCuponYElEnvioEsGratis() throws Exception {
        Cliente cliente = Cliente.builder().nombre("Juan").email("juan@mail.com").build();
        Pedido pedido = Pedido.builder().id(11L).cliente(cliente).build();
        Factura factura = Factura.builder().pedido(pedido).tipoFactura(TipoFactura.C).precioTotal(1234.5).build();

        service.notificarFacturaGenerada(factura);

        MailEnviado mail = mailEnviado();
        assertThat(mail.html()).doesNotContain("Cupón").contains("Gratis", "$ 1.234,50");
    }

    @Test
    void notificarRegistroExitoso_enviaEmailDeBienvenida() throws Exception {
        Cliente cliente = Cliente.builder().nombre("Juan").email("juan@mail.com").build();

        service.notificarRegistroExitoso(cliente);

        MailEnviado mail = mailEnviado();
        assertThat(mail.mensaje().getAllRecipients()).containsExactly(new InternetAddress("juan@mail.com"));
        assertThat(mail.html()).contains("Juan", "juan@mail.com", "http://localhost:4200/productos");
        assertThat(mail.texto()).contains("Juan", "juan@mail.com");
    }

    @Test
    void notificarRecuperacionPassword_llevaElEnlaceEnElBotonYEnTexto() throws Exception {
        Cliente cliente = Cliente.builder().nombre("Juan").email("juan@mail.com").build();
        String enlace = "http://localhost:4200/restablecer-contrasena?token=abc123";

        service.notificarRecuperacionPassword(cliente, enlace, 30);

        MailEnviado mail = mailEnviado();
        assertThat(mail.html()).contains("href=\"" + enlace + "\"", "30 minutos");
        assertThat(mail.texto()).contains(enlace, "30 minutos");
    }

    @Test
    void losDatosDelClienteSeEscapanEnElHtml() throws Exception {
        Cliente cliente = Cliente.builder().nombre("<script>alert(1)</script>").email("juan@mail.com").build();

        service.notificarRegistroExitoso(cliente);

        assertThat(mailEnviado().html())
                .doesNotContain("<script>alert(1)</script>")
                .contains("&lt;script&gt;");
    }

    @Test
    void notificarEnvioActualizado_muestraElEstadoYLaDireccion() throws Exception {
        Cliente cliente = Cliente.builder().nombre("Maria").email("maria@mail.com").build();
        Pedido pedido = Pedido.builder().id(20L).cliente(cliente).build();
        Envio envio = Envio.builder()
                .pedido(pedido)
                .direccion(direccion())
                .estado(EstadoEnvio.EN_CAMINO)
                .fechaSalida(LocalDate.of(2026, 11, 9))
                .fechaLlegada(LocalDate.of(2026, 11, 12))
                .build();

        service.notificarEnvioActualizado(envio);

        MailEnviado mail = mailEnviado();
        assertThat(mail.mensaje().getAllRecipients()).containsExactly(new InternetAddress("maria@mail.com"));
        assertThat(mail.mensaje().getSubject()).contains("20").contains("en camino");
        assertThat(mail.html())
                .contains("Maria", "en camino", "Despachado", "Entregado")
                .contains("Calle 13 567, La Plata (CP 1900)")
                .contains("09/11/2026", "Llegada estimada", "12/11/2026");
        assertThat(mail.texto()).contains("Maria", "En camino", "Calle 13", "La Plata", "1900");
    }

    @Test
    void notificarEnvioActualizado_entregado() throws Exception {
        Cliente cliente = Cliente.builder().nombre("Maria").email("maria@mail.com").build();
        Pedido pedido = Pedido.builder().id(21L).cliente(cliente).build();
        Envio envio = Envio.builder().pedido(pedido).direccion(direccion()).estado(EstadoEnvio.ENTREGADO).build();

        service.notificarEnvioActualizado(envio);

        assertThat(mailEnviado().html()).contains("llegó", "fue entregado");
    }

    @Test
    void siElEnvioDeEmailFalla_noPropagaLaExcepcion() {
        Cliente cliente = Cliente.builder().nombre("Juan").email("juan@mail.com").build();
        Pedido pedido = Pedido.builder().id(1L).cliente(cliente).build();
        Factura factura = Factura.builder()
                .pedido(pedido)
                .tipoFactura(TipoFactura.A)
                .precioTotal(100.0)
                .build();

        doThrow(new MailSendException("error de conexión"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> service.notificarFacturaGenerada(factura)).doesNotThrowAnyException();
    }
}
