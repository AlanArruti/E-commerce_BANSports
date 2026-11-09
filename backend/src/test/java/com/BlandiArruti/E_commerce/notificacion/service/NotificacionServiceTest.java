package com.BlandiArruti.E_commerce.notificacion.service;

import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.entity.Direccion;
import com.BlandiArruti.E_commerce.enums.EstadoEnvio;
import com.BlandiArruti.E_commerce.enums.TipoFactura;
import com.BlandiArruti.E_commerce.envio.entity.Envio;
import com.BlandiArruti.E_commerce.factura.entity.Factura;
import com.BlandiArruti.E_commerce.factura.pdf.FacturaPdfGenerator;
import com.BlandiArruti.E_commerce.geo.entity.Ciudad;
import com.BlandiArruti.E_commerce.notificacion.config.NotificacionProperties;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private NotificacionService service;

    @BeforeEach
    void setUp() {
        NotificacionProperties properties = new NotificacionProperties();
        properties.setRemitente("no-reply@ecommerce-blandi-arruti.com");
        service = new NotificacionService(mailSender, properties, new FacturaPdfGenerator());
    }

    @Test
    void notificarFacturaGenerada_enviaEmailConLaFacturaEnPdfAdjunta() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        Cliente cliente = Cliente.builder().nombre("Juan").apellido("Pérez").email("juan@mail.com").build();
        Pedido pedido = Pedido.builder().id(10L).cliente(cliente).build();
        Factura factura = Factura.builder()
                .id(3L)
                .pedido(pedido)
                .tipoFactura(TipoFactura.B)
                .precioTotal(1500.0)
                .build();

        service.notificarFacturaGenerada(factura);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());

        MimeMessage mensaje = captor.getValue();
        mensaje.saveChanges();
        assertThat(mensaje.getFrom()[0].toString()).isEqualTo("no-reply@ecommerce-blandi-arruti.com");
        assertThat(mensaje.getAllRecipients()[0].toString()).isEqualTo("juan@mail.com");
        assertThat(mensaje.getSubject()).contains("10");

        Multipart partes = (Multipart) mensaje.getContent();
        Part adjunto = null;
        for (int i = 0; i < partes.getCount(); i++) {
            if (Part.ATTACHMENT.equalsIgnoreCase(partes.getBodyPart(i).getDisposition())) {
                adjunto = partes.getBodyPart(i);
            }
        }
        assertThat(adjunto).isNotNull();
        assertThat(adjunto.getFileName()).isEqualTo("factura-B-0001-00000003.pdf");
        assertThat(adjunto.getContentType()).startsWith("application/pdf");
        assertThat(adjunto.getInputStream().readNBytes(5)).isEqualTo("%PDF-".getBytes());
    }

    @Test
    void notificarRegistroExitoso_enviaEmailDeBienvenida() {
        Cliente cliente = Cliente.builder().nombre("Juan").email("juan@mail.com").build();

        service.notificarRegistroExitoso(cliente);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage mensaje = captor.getValue();
        assertThat(mensaje.getFrom()).isEqualTo("no-reply@ecommerce-blandi-arruti.com");
        assertThat(mensaje.getTo()).containsExactly("juan@mail.com");
        assertThat(mensaje.getText())
                .contains("Juan")
                .contains("juan@mail.com");
    }

    @Test
    void notificarEnvioActualizado_enviaEmailConDatosDelEnvio() {
        Ciudad ciudad = Ciudad.builder().nombre("La Plata").build();
        Direccion direccion = Direccion.builder()
                .nombreCalle("Calle 13")
                .numeroCalle(567)
                .codigoPostal("1900")
                .ciudad(ciudad)
                .build();
        Cliente cliente = Cliente.builder().nombre("Maria").email("maria@mail.com").build();
        Pedido pedido = Pedido.builder().id(20L).cliente(cliente).build();
        Envio envio = Envio.builder()
                .pedido(pedido)
                .direccion(direccion)
                .estado(EstadoEnvio.EN_CAMINO)
                .build();

        service.notificarEnvioActualizado(envio);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage mensaje = captor.getValue();
        assertThat(mensaje.getTo()).containsExactly("maria@mail.com");
        assertThat(mensaje.getSubject()).contains("20");
        assertThat(mensaje.getText())
                .contains("Maria")
                .contains("EN_CAMINO")
                .contains("Calle 13")
                .contains("La Plata")
                .contains("1900");
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

        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("error de conexión"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> service.notificarFacturaGenerada(factura)).doesNotThrowAnyException();
    }
}
