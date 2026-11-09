package com.BlandiArruti.E_commerce.factura.pdf;

import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.entity.Direccion;
import com.BlandiArruti.E_commerce.cupon.entity.Cupon;
import com.BlandiArruti.E_commerce.enums.TipoFactura;
import com.BlandiArruti.E_commerce.factura.entity.Factura;
import com.BlandiArruti.E_commerce.geo.entity.Ciudad;
import com.BlandiArruti.E_commerce.pedido.entity.ItemPedido;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import com.BlandiArruti.E_commerce.producto.entity.Variante;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FacturaPdfGeneratorTest {

    private final FacturaPdfGenerator generador = new FacturaPdfGenerator();

    @Test
    void generar_incluyeItemsCuponEnvioYTotal() throws IOException {
        Factura factura = factura(true);

        byte[] pdf = generador.generar(factura);

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        String texto = texto(pdf);
        assertThat(texto)
                .contains("FACTURA B")
                .contains("0001-00000007")
                .contains("Pedido #42")
                .contains("Juan Pérez")
                .contains("Whey Protein (2 kg · Chocolate)")
                .contains("Cupón BIENVENIDA")
                .contains("Envío")
                .contains("sin validez fiscal");
        // 2 × 50.000 = 100.000; cupón 10.000; envío 5.000 → total 95.000
        assertThat(texto).contains("100.000,00").contains("10.000,00").contains("5.000,00").contains("95.000,00");
    }

    @Test
    void generar_retiroSinDireccion_noMuestraEnvio() throws IOException {
        Factura factura = factura(false);

        String texto = texto(generador.generar(factura));

        assertThat(texto).doesNotContain("Envío").doesNotContain("Cupón");
    }

    @Test
    void nombreArchivo_llevaTipoYNumero() {
        assertThat(generador.nombreArchivo(factura(true))).isEqualTo("factura-B-0001-00000007.pdf");
    }

    private static Factura factura(boolean conEnvioYCupon) {
        Map<String, String> atributos = new LinkedHashMap<>();
        atributos.put("peso", "2 kg");
        atributos.put("sabor", "Chocolate");
        Producto producto = Producto.builder().id(1L).nombre("Whey Protein").build();
        Variante variante = Variante.builder().id(4L).atributos(atributos).build();
        ItemPedido item = ItemPedido.builder()
                .producto(producto).variante(variante).cantidad(2).precioProducto(100_000.0).build();

        Cliente cliente = Cliente.builder()
                .id(1L).nombre("Juan").apellido("Pérez").dni("30111222").email("juan@mail.com").build();
        Pedido.PedidoBuilder pedido = Pedido.builder().id(42L).cliente(cliente);
        double total = 100_000.0;
        if (conEnvioYCupon) {
            Direccion direccion = Direccion.builder()
                    .nombreCalle("Av. Colón").numeroCalle(1234).codigoPostal("7600")
                    .ciudad(Ciudad.builder().nombre("Mar del Plata").build()).build();
            pedido.direccionEnvio(direccion)
                    .costoEnvio(5_000.0)
                    .cupon(Cupon.builder().codigo("BIENVENIDA").build())
                    .descuentoCupon(10_000.0);
            total = 95_000.0;
        } else {
            pedido.costoEnvio(0.0).descuentoCupon(0.0);
        }
        Pedido armado = pedido.build();
        armado.getItems().add(item);

        return Factura.builder()
                .id(7L)
                .pedido(armado)
                .tipoFactura(TipoFactura.B)
                .precioTotal(total)
                .fechaEmision(LocalDateTime.of(2026, 11, 9, 15, 30))
                .build();
    }

    private static String texto(byte[] pdf) throws IOException {
        PdfReader lector = new PdfReader(pdf);
        StringBuilder texto = new StringBuilder();
        PdfTextExtractor extractor = new PdfTextExtractor(lector);
        for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
            texto.append(extractor.getTextFromPage(pagina)).append('\n');
        }
        lector.close();
        return texto.toString();
    }
}
