package com.BlandiArruti.E_commerce.config;

import com.BlandiArruti.E_commerce.administrador.entity.Administrador;
import com.BlandiArruti.E_commerce.administrador.repository.AdministradorRepository;
import com.BlandiArruti.E_commerce.categoria.entity.Categoria;
import com.BlandiArruti.E_commerce.categoria.repository.CategoriaRepository;
import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.entity.Direccion;
import com.BlandiArruti.E_commerce.cliente.repository.ClienteRepository;
import com.BlandiArruti.E_commerce.cliente.repository.DireccionRepository;
import com.BlandiArruti.E_commerce.envio.entity.Envio;
import com.BlandiArruti.E_commerce.envio.repository.EnvioRepository;
import com.BlandiArruti.E_commerce.enums.EstadoEnvio;
import com.BlandiArruti.E_commerce.enums.EstadoPedido;
import com.BlandiArruti.E_commerce.enums.TipoFactura;
import com.BlandiArruti.E_commerce.factura.entity.Factura;
import com.BlandiArruti.E_commerce.factura.repository.FacturaRepository;
import com.BlandiArruti.E_commerce.geo.entity.Ciudad;
import com.BlandiArruti.E_commerce.geo.entity.Pais;
import com.BlandiArruti.E_commerce.geo.entity.Provincia;
import com.BlandiArruti.E_commerce.geo.repository.CiudadRepository;
import com.BlandiArruti.E_commerce.geo.repository.PaisRepository;
import com.BlandiArruti.E_commerce.geo.repository.ProvinciaRepository;
import com.BlandiArruti.E_commerce.pedido.entity.ItemPedido;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import com.BlandiArruti.E_commerce.pedido.repository.ItemPedidoRepository;
import com.BlandiArruti.E_commerce.pedido.repository.PedidoRepository;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import com.BlandiArruti.E_commerce.producto.entity.Variante;
import com.BlandiArruti.E_commerce.producto.repository.ProductoRepository;
import com.BlandiArruti.E_commerce.producto.repository.VarianteRepository;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final AdministradorRepository administradorRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final PaisRepository paisRepository;
    private final ProvinciaRepository provinciaRepository;
    private final CiudadRepository ciudadRepository;
    private final DireccionRepository direccionRepository;
    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final PedidoRepository pedidoRepository;
    private final ItemPedidoRepository itemPedidoRepository;
    private final FacturaRepository facturaRepository;
    private final CategoriaRepository categoriaRepository;
    private final EnvioRepository envioRepository;

    @Override
    public void run(String... args) throws Exception {

        if (paisRepository.count() > 0) {
            return;
        }

        //------------GEO------------//
        Pais argentina = paisRepository.save(
                Pais.builder().nombre("Argentina").build()
        );
        cargarProvinciasYCiudadesArgentina(argentina);

        //------------CATEGORIA------------//
        Categoria electronica = categoriaRepository.save(
                Categoria.builder().nombre("Electrónica").build()
        );
        Categoria indumentaria = categoriaRepository.save(
                Categoria.builder().nombre("Indumentaria").build()
        );
        Categoria hogar = categoriaRepository.save(
                Categoria.builder().nombre("Hogar").build()
        );
        Categoria deportes = categoriaRepository.save(
                Categoria.builder().nombre("Deportes").build()
        );

        //------------PRODUCTO------------//
        Producto laptop = productoRepository.save(Producto.builder()
                .nombre("Laptop")
                .descripcion("Laptop con procesador Intel i7 y 16GB de RAM.")
                .precio(999.99)
                .categoria(electronica)
                .build());
        Producto smartphone = productoRepository.save(Producto.builder()
                .nombre("Smartphone")
                .descripcion("Smartphone Android con pantalla OLED de 6.5 pulgadas.")
                .precio(699.99)
                .categoria(electronica)
                .build());
        Producto remera = productoRepository.save(Producto.builder()
                .nombre("Remera básica")
                .descripcion("Remera de algodón 100% color sólido.")
                .precio(4999.99)
                .categoria(indumentaria)
                .build());
        Producto silla = productoRepository.save(Producto.builder()
                .nombre("Silla gamer")
                .descripcion("Silla ergonómica con apoyabrazos ajustables.")
                .precio(199.99)
                .categoria(hogar)
                .build());
        Producto pelota = productoRepository.save(Producto.builder()
                .nombre("Pelota fútbol")
                .descripcion("Pelota oficial talle 5.")
                .precio(89.99)
                .categoria(deportes)
                .build());

        // Catálogo extra: le da cuerpo al front (paginación, filtros y orden reales).
        Producto auriculares = productoRepository.save(Producto.builder()
                .nombre("Auriculares inalámbricos")
                .descripcion("Bluetooth 5.3 con cancelación activa de ruido y 30h de batería.")
                .precio(149.99)
                .categoria(electronica)
                .build());
        Producto tablet = productoRepository.save(Producto.builder()
                .nombre("Tablet 11 pulgadas")
                .descripcion("Pantalla 2K, 128GB de almacenamiento y lápiz óptico incluido.")
                .precio(429.99)
                .categoria(electronica)
                .build());
        Producto teclado = productoRepository.save(Producto.builder()
                .nombre("Teclado mecánico")
                .descripcion("Switches rojos, retroiluminación RGB y layout español.")
                .precio(89.99)
                .categoria(electronica)
                .build());
        Producto campera = productoRepository.save(Producto.builder()
                .nombre("Campera rompeviento")
                .descripcion("Impermeable liviana con capucha desmontable.")
                .precio(24999.99)
                .categoria(indumentaria)
                .build());
        Producto jean = productoRepository.save(Producto.builder()
                .nombre("Jean recto")
                .descripcion("Denim rígido de algodón, corte clásico.")
                .precio(18999.99)
                .categoria(indumentaria)
                .build());
        Producto zapatillas = productoRepository.save(Producto.builder()
                .nombre("Zapatillas urbanas")
                .descripcion("Suela de goma antideslizante y plantilla acolchada.")
                .precio(34999.99)
                .categoria(indumentaria)
                .build());
        Producto lampara = productoRepository.save(Producto.builder()
                .nombre("Lámpara de escritorio LED")
                .descripcion("Tres temperaturas de color y brazo articulado.")
                .precio(39.99)
                .categoria(hogar)
                .build());
        Producto cafetera = productoRepository.save(Producto.builder()
                .nombre("Cafetera de filtro")
                .descripcion("Jarra de vidrio de 1.2L con placa calefactora.")
                .precio(74.99)
                .categoria(hogar)
                .build());
        Producto juegoSabanas = productoRepository.save(Producto.builder()
                .nombre("Juego de sábanas")
                .descripcion("200 hilos, incluye funda de almohada.")
                .precio(29999.99)
                .categoria(hogar)
                .build());
        Producto mancuernas = productoRepository.save(Producto.builder()
                .nombre("Set de mancuernas")
                .descripcion("Par regulable de 2 a 10 kg con recubrimiento de goma.")
                .precio(129.99)
                .categoria(deportes)
                .build());
        Producto bicicleta = productoRepository.save(Producto.builder()
                .nombre("Bicicleta mountain bike")
                .descripcion("Rodado 29, 21 velocidades y frenos a disco.")
                .precio(899.99)
                .categoria(deportes)
                .build());
        Producto matYoga = productoRepository.save(Producto.builder()
                .nombre("Mat de yoga")
                .descripcion("6mm de espesor, antideslizante, con correa de transporte.")
                .precio(34.99)
                .categoria(deportes)
                .build());

        //------------VARIANTES------------//
        Variante laptopPlata = varianteRepository.save(Variante.builder()
                .producto(laptop)
                .atributos(Map.of("color", "Plata", "ram", "16GB"))
                .stock(10)
                .build());
        varianteRepository.save(Variante.builder()
                .producto(laptop)
                .atributos(Map.of("color", "Negro", "ram", "32GB"))
                .stock(5)
                .build());
        Variante remeraM = varianteRepository.save(Variante.builder()
                .producto(remera)
                .atributos(Map.of("talla", "M", "color", "Blanco"))
                .stock(50)
                .build());
        varianteRepository.save(Variante.builder()
                .producto(remera)
                .atributos(Map.of("talla", "M", "color", "Negro"))
                .stock(0)
                .build());
        varianteRepository.save(Variante.builder()
                .producto(remera)
                .atributos(Map.of("talla", "L", "color", "Blanco"))
                .stock(2)
                .build());
        Variante sillaRoja = varianteRepository.save(Variante.builder()
                .producto(silla)
                .atributos(Map.of("color", "Rojo"))
                .stock(15)
                .build());

        // Variantes de los productos del catálogo extra.
        varianteRepository.save(Variante.builder()
                .producto(smartphone).atributos(Map.of("color", "Negro", "almacenamiento", "128GB")).stock(12).build());
        varianteRepository.save(Variante.builder()
                .producto(smartphone).atributos(Map.of("color", "Azul", "almacenamiento", "256GB")).stock(6).build());
        varianteRepository.save(Variante.builder()
                .producto(pelota).atributos(Map.of("color", "Blanco")).stock(30).build());
        varianteRepository.save(Variante.builder()
                .producto(auriculares).atributos(Map.of("color", "Negro")).stock(25).build());
        varianteRepository.save(Variante.builder()
                .producto(auriculares).atributos(Map.of("color", "Blanco")).stock(8).build());
        varianteRepository.save(Variante.builder()
                .producto(tablet).atributos(Map.of("color", "Gris", "almacenamiento", "128GB")).stock(9).build());
        varianteRepository.save(Variante.builder()
                .producto(teclado).atributos(Map.of("switch", "Rojo", "layout", "Español")).stock(18).build());
        varianteRepository.save(Variante.builder()
                .producto(campera).atributos(Map.of("talla", "M", "color", "Negro")).stock(14).build());
        varianteRepository.save(Variante.builder()
                .producto(campera).atributos(Map.of("talla", "L", "color", "Verde")).stock(7).build());
        varianteRepository.save(Variante.builder()
                .producto(jean).atributos(Map.of("talla", "40", "color", "Azul")).stock(20).build());
        varianteRepository.save(Variante.builder()
                .producto(jean).atributos(Map.of("talla", "42", "color", "Negro")).stock(11).build());
        varianteRepository.save(Variante.builder()
                .producto(zapatillas).atributos(Map.of("talla", "41", "color", "Blanco")).stock(10).build());
        varianteRepository.save(Variante.builder()
                .producto(zapatillas).atributos(Map.of("talla", "43", "color", "Negro")).stock(4).build());
        varianteRepository.save(Variante.builder()
                .producto(lampara).atributos(Map.of("color", "Blanco")).stock(22).build());
        varianteRepository.save(Variante.builder()
                .producto(cafetera).atributos(Map.of("color", "Acero")).stock(13).build());
        varianteRepository.save(Variante.builder()
                .producto(juegoSabanas).atributos(Map.of("medida", "Queen", "color", "Gris")).stock(16).build());
        varianteRepository.save(Variante.builder()
                .producto(mancuernas).atributos(Map.of("peso", "10kg")).stock(9).build());
        varianteRepository.save(Variante.builder()
                .producto(bicicleta).atributos(Map.of("rodado", "29", "color", "Negro")).stock(3).build());
        varianteRepository.save(Variante.builder()
                .producto(matYoga).atributos(Map.of("color", "Violeta")).stock(28).build());

        //------------ADMIN------------//
        administradorRepository.save(Administrador.builder()
                .username("admin")
                .contrasenia(passwordEncoder.encode("admin123"))
                .build());

        //------------CLIENTES------------//
        String hashPassword = passwordEncoder.encode("password123");

        Cliente juan = clienteRepository.save(Cliente.builder()
                .nombre("Juan")
                .apellido("Pérez")
                .dni("30111222")
                .email("juan@mail.com")
                .contrasenia(hashPassword)
                .build());
        Cliente maria = clienteRepository.save(Cliente.builder()
                .nombre("María")
                .apellido("González")
                .dni("28999888")
                .email("maria@mail.com")
                .contrasenia(hashPassword)
                .build());
        Cliente pedro = clienteRepository.save(Cliente.builder()
                .nombre("Pedro")
                .apellido("López")
                .dni("35444555")
                .email("pedro@mail.com")
                .contrasenia(hashPassword)
                .build());

        //------------DIRECCIONES------------//
        Ciudad marDelPlata = ciudadRepository.findByNombre("Mar del Plata")
                .orElseThrow(() -> new EntidadNoEncontradaException("Ciudad 'Mar del Plata' no encontrada."));
        Ciudad laPlata = ciudadRepository.findByNombre("La Plata")
                .orElseThrow(() -> new EntidadNoEncontradaException("Ciudad 'La Plata' no encontrada."));
        Ciudad cordoba = ciudadRepository.findByNombre("Córdoba")
                .orElseThrow(() -> new EntidadNoEncontradaException("Ciudad 'Córdoba' no encontrada."));

        direccionRepository.save(Direccion.builder()
                .nombreCalle("Av. Colón")
                .numeroCalle(1234)
                .codigoPostal("7600")
                .ciudad(marDelPlata)
                .cliente(juan)
                .build());
        direccionRepository.save(Direccion.builder()
                .nombreCalle("Calle 13")
                .numeroCalle(567)
                .codigoPostal("1900")
                .ciudad(laPlata)
                .cliente(maria)
                .build());
        Direccion dirPedro = direccionRepository.save(Direccion.builder()
                .nombreCalle("Bv. San Juan")
                .numeroCalle(890)
                .codigoPostal("5000")
                .ciudad(cordoba)
                .cliente(pedro)
                .build());

        //------------PEDIDOS------------//
        Pedido pedido1 = pedidoRepository.save(Pedido.builder()
                .cliente(juan)
                .estadoPedido(EstadoPedido.PENDIENTE_PAGO)
                .build());
        Pedido pedido2 = pedidoRepository.save(Pedido.builder()
                .cliente(maria)
                .estadoPedido(EstadoPedido.PAGADO)
                .build());
        Pedido pedido3 = pedidoRepository.save(Pedido.builder()
                .cliente(pedro)
                .estadoPedido(EstadoPedido.ENTREGADO)
                .build());

        //------------ITEMS PEDIDO------------//
        itemPedidoRepository.save(ItemPedido.builder()
                .pedido(pedido1)
                .variante(laptopPlata)
                .producto(laptop)
                .cantidad(1)
                .precioProducto(laptop.getPrecio())
                .build());
        itemPedidoRepository.save(ItemPedido.builder()
                .pedido(pedido2)
                .variante(remeraM)
                .producto(remera)
                .cantidad(3)
                .precioProducto(remera.getPrecio())
                .build());
        itemPedidoRepository.save(ItemPedido.builder()
                .pedido(pedido3)
                .variante(sillaRoja)
                .producto(silla)
                .cantidad(2)
                .precioProducto(silla.getPrecio())
                .build());

        //------------ENVIOS------------//
        // pedido1 (PENDIENTE_PAGO): sin envio
        // pedido2 (PAGADO): sin envio aun
        // pedido3 (ENTREGADO): tiene envio entregado
        envioRepository.save(Envio.builder()
                .pedido(pedido3)
                .direccion(dirPedro)
                .estado(EstadoEnvio.ENTREGADO)
                .fechaSalida(LocalDate.now().minusDays(5))
                .fechaLlegada(LocalDate.now().minusDays(2))
                .build());

        //------------FACTURAS------------//
        // pedido1 (PENDIENTE_PAGO): sin factura
        // pedido2 (PAGADO): factura generada al pagar
        // pedido3 (ENTREGADO): factura generada al pagar
        facturaRepository.save(Factura.builder()
                .pedido(pedido2)
                .tipoFactura(TipoFactura.B)
                .precioTotal(remera.getPrecio() * 3)
                .build());
        facturaRepository.save(Factura.builder()
                .pedido(pedido3)
                .tipoFactura(TipoFactura.A)
                .precioTotal(silla.getPrecio() * 2)
                .build());
    }

    // Carga las 24 provincias de Argentina y sus ciudades desde un archivo
    // de texto (formato "Provincia;Ciudad" por linea) para no tener que
    // escribirlas a mano una por una.
    private void cargarProvinciasYCiudadesArgentina(Pais argentina) throws Exception {
        Map<String, Provincia> provincias = new HashMap<>();

        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/data/argentina-geo.csv"), StandardCharsets.UTF_8))) {

            String linea;
            while ((linea = lector.readLine()) != null) {
                String[] partes = linea.split(";");
                String nombreProvincia = partes[0];
                String nombreCiudad = partes[1];

                Provincia provincia = provincias.computeIfAbsent(nombreProvincia, nombre ->
                        provinciaRepository.save(Provincia.builder().nombre(nombre).pais(argentina).build()));

                ciudadRepository.save(Ciudad.builder().nombre(nombreCiudad).provincia(provincia).build());
            }
        }
    }
}
