package com.BlandiArruti.E_commerce.factura.pdf;

import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.entity.Direccion;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.factura.entity.Factura;
import com.BlandiArruti.E_commerce.pedido.entity.ItemPedido;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Arma el PDF de una factura: emisor, cliente, ítems, cupón, envío y total.
 * Los importes salen del pedido igual que en PedidoService.calcularTotal, así el PDF
 * dice lo mismo que se cobró.
 *
 * No es un comprobante fiscal: no tiene CAE de ARCA, y el PDF lo aclara.
 */
@Component
public class FacturaPdfGenerator {

    private static final Color VERDE = new Color(0x2b, 0xe3, 0x7a);
    private static final Color NEGRO = new Color(0x06, 0x08, 0x07);
    private static final Color GRIS = new Color(0x6b, 0x72, 0x80);
    private static final Color GRIS_CLARO = new Color(0xf3, 0xf4, 0xf6);

    private static final Font TITULO = new Font(Font.HELVETICA, 22, Font.BOLD, NEGRO);
    private static final Font MARCA = new Font(Font.HELVETICA, 22, Font.BOLD, VERDE);
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 11, Font.BOLD, NEGRO);
    private static final Font NORMAL = new Font(Font.HELVETICA, 10, Font.NORMAL, NEGRO);
    private static final Font CHICO = new Font(Font.HELVETICA, 8, Font.NORMAL, GRIS);
    private static final Font ENCABEZADO_TABLA = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
    private static final Font TOTAL = new Font(Font.HELVETICA, 13, Font.BOLD, NEGRO);

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Nombre del archivo adjunto: factura-B-0001-00000012.pdf */
    public String nombreArchivo(Factura factura) {
        return "factura-" + factura.getTipoFactura() + "-" + numero(factura) + ".pdf";
    }

    public byte[] generar(Factura factura) {
        Pedido pedido = factura.getPedido();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.addTitle("Factura " + factura.getTipoFactura() + " " + numero(factura));
            documento.addAuthor("BAN Sports");
            documento.open();

            documento.add(encabezado(factura));
            documento.add(espacio(14));
            documento.add(datosCliente(pedido));
            documento.add(espacio(14));
            documento.add(tablaItems(pedido));
            documento.add(espacio(8));
            documento.add(totales(pedido, factura));
            documento.add(espacio(24));
            documento.add(new Paragraph(
                    "Comprobante sin validez fiscal: no tiene CAE de ARCA. Generado por la tienda BAN Sports "
                            + "(proyecto académico, UTN Mar del Plata).", CHICO));

            documento.close();
            return salida.toByteArray();
        } catch (DocumentException e) {
            throw new EcommerceException("No se pudo generar el PDF de la factura " + numero(factura) + ".", e);
        }
    }

    private PdfPTable encabezado(Factura factura) {
        PdfPTable tabla = new PdfPTable(new float[]{3, 2});
        tabla.setWidthPercentage(100);

        Phrase marca = new Phrase();
        marca.add(new Chunk("BAN ", MARCA));
        marca.add(new Chunk("SPORTS", TITULO));
        PdfPCell emisor = sinBorde(new PdfPCell());
        emisor.addElement(new Paragraph(marca));
        emisor.addElement(new Paragraph("Suplementos deportivos", NORMAL));
        tabla.addCell(emisor);

        PdfPCell datos = sinBorde(new PdfPCell());
        Paragraph tipo = new Paragraph("FACTURA " + factura.getTipoFactura(), TITULO);
        tipo.setAlignment(Element.ALIGN_RIGHT);
        datos.addElement(tipo);
        datos.addElement(derecha("N° " + numero(factura), SUBTITULO));
        datos.addElement(derecha("Fecha: " + fecha(factura), NORMAL));
        datos.addElement(derecha("Pedido #" + factura.getPedido().getId(), NORMAL));
        tabla.addCell(datos);

        PdfPCell linea = sinBorde(new PdfPCell());
        linea.setColspan(2);
        linea.setBorder(Rectangle.BOTTOM);
        linea.setBorderColor(VERDE);
        linea.setBorderWidth(2);
        linea.setFixedHeight(10);
        tabla.addCell(linea);
        return tabla;
    }

    private PdfPTable datosCliente(Pedido pedido) {
        Cliente cliente = pedido.getCliente();
        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        PdfPCell celda = sinBorde(new PdfPCell());
        celda.setBackgroundColor(GRIS_CLARO);
        celda.setPadding(10);
        celda.addElement(new Paragraph("Cliente", SUBTITULO));
        celda.addElement(new Paragraph(cliente.getNombre() + " " + cliente.getApellido(), NORMAL));
        if (cliente.getDni() != null && !cliente.getDni().isBlank()) {
            celda.addElement(new Paragraph("DNI: " + cliente.getDni(), NORMAL));
        }
        celda.addElement(new Paragraph("Email: " + cliente.getEmail(), NORMAL));
        Direccion direccion = pedido.getDireccionEnvio();
        if (direccion != null) {
            celda.addElement(new Paragraph("Envío a: " + direccion(direccion), NORMAL));
        }
        tabla.addCell(celda);
        return tabla;
    }

    private PdfPTable tablaItems(Pedido pedido) {
        PdfPTable tabla = new PdfPTable(new float[]{5, 1.2f, 2, 2});
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        for (String titulo : new String[]{"Producto", "Cant.", "Precio unit.", "Subtotal"}) {
            PdfPCell celda = new PdfPCell(new Phrase(titulo, ENCABEZADO_TABLA));
            celda.setBackgroundColor(NEGRO);
            celda.setPadding(6);
            celda.setBorder(Rectangle.NO_BORDER);
            celda.setHorizontalAlignment(titulo.equals("Producto") ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            tabla.addCell(celda);
        }
        for (ItemPedido item : pedido.getItems()) {
            // precioProducto es el total de la línea (precio con oferta × cantidad).
            double subtotal = item.getPrecioProducto();
            double unitario = item.getCantidad() > 0 ? subtotal / item.getCantidad() : subtotal;
            tabla.addCell(celdaItem(descripcion(item), Element.ALIGN_LEFT));
            tabla.addCell(celdaItem(String.valueOf(item.getCantidad()), Element.ALIGN_RIGHT));
            tabla.addCell(celdaItem(pesos(unitario), Element.ALIGN_RIGHT));
            tabla.addCell(celdaItem(pesos(subtotal), Element.ALIGN_RIGHT));
        }
        return tabla;
    }

    private PdfPTable totales(Pedido pedido, Factura factura) {
        double productos = pedido.getItems().stream().mapToDouble(ItemPedido::getPrecioProducto).sum();
        double cupon = pedido.getDescuentoCupon() != null ? pedido.getDescuentoCupon() : 0.0;
        double envio = pedido.getCostoEnvio() != null ? pedido.getCostoEnvio() : 0.0;

        PdfPTable tabla = new PdfPTable(new float[]{7, 2});
        tabla.setWidthPercentage(100);
        filaTotal(tabla, "Subtotal productos", pesos(productos), NORMAL);
        if (cupon > 0) {
            String etiqueta = pedido.getCupon() != null
                    ? "Cupón " + pedido.getCupon().getCodigo()
                    : "Cupón";
            filaTotal(tabla, etiqueta, "- " + pesos(cupon), NORMAL);
        }
        if (pedido.getDireccionEnvio() != null) {
            filaTotal(tabla, "Envío", envio > 0 ? pesos(envio) : "Sin cargo", NORMAL);
        }
        PdfPCell etiquetaTotal = sinBorde(new PdfPCell(new Phrase("TOTAL", TOTAL)));
        etiquetaTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        etiquetaTotal.setBorder(Rectangle.TOP);
        etiquetaTotal.setBorderColor(VERDE);
        etiquetaTotal.setPaddingTop(6);
        tabla.addCell(etiquetaTotal);
        PdfPCell montoTotal = sinBorde(new PdfPCell(new Phrase(pesos(factura.getPrecioTotal()), TOTAL)));
        montoTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        montoTotal.setBorder(Rectangle.TOP);
        montoTotal.setBorderColor(VERDE);
        montoTotal.setPaddingTop(6);
        tabla.addCell(montoTotal);
        return tabla;
    }

    private static void filaTotal(PdfPTable tabla, String etiqueta, String monto, Font fuente) {
        PdfPCell izquierda = sinBorde(new PdfPCell(new Phrase(etiqueta, fuente)));
        izquierda.setHorizontalAlignment(Element.ALIGN_RIGHT);
        izquierda.setPaddingBottom(4);
        tabla.addCell(izquierda);
        PdfPCell derecha = sinBorde(new PdfPCell(new Phrase(monto, fuente)));
        derecha.setHorizontalAlignment(Element.ALIGN_RIGHT);
        derecha.setPaddingBottom(4);
        tabla.addCell(derecha);
    }

    private static PdfPCell celdaItem(String texto, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, NORMAL));
        celda.setPadding(6);
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderColor(GRIS_CLARO);
        celda.setHorizontalAlignment(alineacion);
        return celda;
    }

    private static PdfPCell sinBorde(PdfPCell celda) {
        celda.setBorder(Rectangle.NO_BORDER);
        return celda;
    }

    private static Paragraph derecha(String texto, Font fuente) {
        Paragraph parrafo = new Paragraph(texto, fuente);
        parrafo.setAlignment(Element.ALIGN_RIGHT);
        return parrafo;
    }

    private static Paragraph espacio(float alto) {
        Paragraph parrafo = new Paragraph(" ");
        parrafo.setSpacingAfter(alto);
        parrafo.setLeading(0);
        return parrafo;
    }

    /** Producto y variante: "Whey Protein Concentrada (2 kg · Chocolate)". */
    private static String descripcion(ItemPedido item) {
        String nombre = item.getProducto() != null ? item.getProducto().getNombre() : "Producto";
        Map<String, String> atributos = item.getVariante() != null ? item.getVariante().getAtributos() : null;
        if (atributos == null || atributos.isEmpty()) {
            return nombre;
        }
        return nombre + " (" + atributos.values().stream().collect(Collectors.joining(" · ")) + ")";
    }

    private static String direccion(Direccion direccion) {
        StringBuilder texto = new StringBuilder()
                .append(direccion.getNombreCalle()).append(' ').append(direccion.getNumeroCalle());
        if (direccion.getCiudad() != null) {
            texto.append(", ").append(direccion.getCiudad().getNombre());
        }
        if (direccion.getCodigoPostal() != null) {
            texto.append(" (CP ").append(direccion.getCodigoPostal()).append(')');
        }
        return texto.toString();
    }

    /** Punto de venta fijo 0001 y el id de la factura: 0001-00000012. */
    private static String numero(Factura factura) {
        return "0001-%08d".formatted(factura.getId() != null ? factura.getId() : 0L);
    }

    private static String fecha(Factura factura) {
        LocalDateTime emision = factura.getFechaEmision();
        return emision != null ? emision.format(FECHA) : "—";
    }

    private static String pesos(double monto) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(Locale.of("es", "AR"));
        return formato.format(monto);
    }
}
