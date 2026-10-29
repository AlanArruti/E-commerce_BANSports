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
        Categoria proteinas = categoriaRepository.save(
                Categoria.builder().nombre("Proteínas").build()
        );
        Categoria creatinas = categoriaRepository.save(
                Categoria.builder().nombre("Creatinas").build()
        );
        Categoria preEntrenos = categoriaRepository.save(
                Categoria.builder().nombre("Pre-entrenos").build()
        );
        Categoria aminoacidos = categoriaRepository.save(
                Categoria.builder().nombre("Aminoácidos").build()
        );
        Categoria vitaminas = categoriaRepository.save(
                Categoria.builder().nombre("Vitaminas").build()
        );

        //------------PRODUCTO------------//
        // El precio es por producto, así que cada producto tiene una sola presentación
        // y las variantes cambian solo el sabor.
        Producto wheyConcentrada = productoRepository.save(Producto.builder()
                .nombre("Whey Protein Concentrada")
                .descripcion("24 g de proteína por servicio. Ideal para recuperación y ganancia de masa muscular.")
                .precio(42999.99)
                .categoria(proteinas)
                .build());
        Producto wheyIsolate = productoRepository.save(Producto.builder()
                .nombre("Whey Protein Isolate")
                .descripcion("Proteína aislada de rápida absorción, baja en grasas y sin lactosa.")
                .precio(58999.99)
                .categoria(proteinas)
                .build());
        Producto proteinaVegana = productoRepository.save(Producto.builder()
                .nombre("Proteína Vegana")
                .descripcion("Blend de arveja y arroz con 21 g de proteína por servicio. Apta veganos.")
                .precio(46999.99)
                .categoria(proteinas)
                .build());
        Producto caseina = productoRepository.save(Producto.builder()
                .nombre("Caseína Micelar")
                .descripcion("Proteína de absorción lenta, pensada para tomar antes de dormir.")
                .precio(51999.99)
                .categoria(proteinas)
                .build());
        Producto ganador = productoRepository.save(Producto.builder()
                .nombre("Ganador de Peso Mass Gainer")
                .descripcion("Carbohidratos y proteína en proporción 3:1 para sumar calorías de calidad.")
                .precio(38999.99)
                .categoria(proteinas)
                .build());
        Producto creatinaMono = productoRepository.save(Producto.builder()
                .nombre("Creatina Monohidrato")
                .descripcion("Creatina pura de 5 g por servicio para más fuerza y potencia.")
                .precio(27999.99)
                .categoria(creatinas)
                .build());
        Producto creatinaMicro = productoRepository.save(Producto.builder()
                .nombre("Creatina Micronizada")
                .descripcion("Partícula ultrafina que se disuelve fácil y no deja grumos.")
                .precio(39999.99)
                .categoria(creatinas)
                .build());
        Producto creatinaCapsulas = productoRepository.save(Producto.builder()
                .nombre("Creatina en Cápsulas")
                .descripcion("La dosis diaria lista para llevar, sin medir ni mezclar.")
                .precio(24999.99)
                .categoria(creatinas)
                .build());
        Producto preExplosive = productoRepository.save(Producto.builder()
                .nombre("Pre-entreno Explosive")
                .descripcion("Cafeína, beta-alanina y citrulina para entrenar con energía y enfoque.")
                .precio(32999.99)
                .categoria(preEntrenos)
                .build());
        Producto preSinCafeina = productoRepository.save(Producto.builder()
                .nombre("Pre-entreno Sin Cafeína")
                .descripcion("Bombeo y resistencia sin estimulantes. Se puede tomar de noche.")
                .precio(29999.99)
                .categoria(preEntrenos)
                .build());
        Producto oxidoNitrico = productoRepository.save(Producto.builder()
                .nombre("Óxido Nítrico Pump")
                .descripcion("Arginina y citrulina para mejorar el bombeo y la vascularización.")
                .precio(26999.99)
                .categoria(preEntrenos)
                .build());
        Producto bcaa = productoRepository.save(Producto.builder()
                .nombre("BCAA 2:1:1")
                .descripcion("Leucina, isoleucina y valina para cuidar el músculo durante el entrenamiento.")
                .precio(21999.99)
                .categoria(aminoacidos)
                .build());
        Producto glutamina = productoRepository.save(Producto.builder()
                .nombre("Glutamina")
                .descripcion("L-glutamina pura para la recuperación muscular y el sistema inmune.")
                .precio(19999.99)
                .categoria(aminoacidos)
                .build());
        Producto eaa = productoRepository.save(Producto.builder()
                .nombre("Aminoácidos Esenciales EAA")
                .descripcion("Los 9 aminoácidos esenciales en una sola toma.")
                .precio(28999.99)
                .categoria(aminoacidos)
                .build());
        Producto multivitaminico = productoRepository.save(Producto.builder()
                .nombre("Multivitamínico Deportivo")
                .descripcion("Vitaminas y minerales para cubrir las necesidades de quien entrena.")
                .precio(15999.99)
                .categoria(vitaminas)
                .build());
        Producto omega3 = productoRepository.save(Producto.builder()
                .nombre("Omega 3")
                .descripcion("Aceite de pescado con EPA y DHA para la salud cardiovascular.")
                .precio(17999.99)
                .categoria(vitaminas)
                .build());
        Producto magnesio = productoRepository.save(Producto.builder()
                .nombre("Magnesio + Zinc")
                .descripcion("Ayuda al descanso, la recuperación y la función muscular.")
                .precio(13999.99)
                .categoria(vitaminas)
                .build());

        //------------VARIANTES------------//
        Variante wheyChocolate = varianteRepository.save(Variante.builder()
                .producto(wheyConcentrada)
                .atributos(Map.of("presentacion", "1 kg", "sabor", "Chocolate"))
                .stock(25)
                .build());
        varianteRepository.save(Variante.builder()
                .producto(wheyConcentrada)
                .atributos(Map.of("presentacion", "1 kg", "sabor", "Vainilla"))
                .stock(18)
                .build());
        varianteRepository.save(Variante.builder()
                .producto(wheyConcentrada)
                .atributos(Map.of("presentacion", "1 kg", "sabor", "Frutilla"))
                .stock(0)
                .build());
        Variante creatinaSinSabor = varianteRepository.save(Variante.builder()
                .producto(creatinaMono)
                .atributos(Map.of("presentacion", "300 g", "sabor", "Sin sabor"))
                .stock(40)
                .build());
        Variante preFrutosRojos = varianteRepository.save(Variante.builder()
                .producto(preExplosive)
                .atributos(Map.of("presentacion", "300 g", "sabor", "Frutos rojos"))
                .stock(15)
                .build());
        varianteRepository.save(Variante.builder()
                .producto(preExplosive)
                .atributos(Map.of("presentacion", "300 g", "sabor", "Limón"))
                .stock(3)
                .build());

        // Stock variado (incluye agotados y pocas unidades) para probar el catálogo.
        varianteRepository.save(Variante.builder()
                .producto(wheyIsolate).atributos(Map.of("presentacion", "900 g", "sabor", "Chocolate")).stock(10).build());
        varianteRepository.save(Variante.builder()
                .producto(wheyIsolate).atributos(Map.of("presentacion", "900 g", "sabor", "Cookies and cream")).stock(6).build());
        varianteRepository.save(Variante.builder()
                .producto(proteinaVegana).atributos(Map.of("presentacion", "1 kg", "sabor", "Chocolate")).stock(9).build());
        varianteRepository.save(Variante.builder()
                .producto(caseina).atributos(Map.of("presentacion", "1 kg", "sabor", "Vainilla")).stock(7).build());
        varianteRepository.save(Variante.builder()
                .producto(ganador).atributos(Map.of("presentacion", "3 kg", "sabor", "Chocolate")).stock(12).build());
        varianteRepository.save(Variante.builder()
                .producto(ganador).atributos(Map.of("presentacion", "3 kg", "sabor", "Banana")).stock(0).build());
        varianteRepository.save(Variante.builder()
                .producto(creatinaMicro).atributos(Map.of("presentacion", "500 g", "sabor", "Sin sabor")).stock(20).build());
        varianteRepository.save(Variante.builder()
                .producto(creatinaCapsulas).atributos(Map.of("presentacion", "120 cápsulas")).stock(14).build());
        varianteRepository.save(Variante.builder()
                .producto(preSinCafeina).atributos(Map.of("presentacion", "300 g", "sabor", "Uva")).stock(11).build());
        varianteRepository.save(Variante.builder()
                .producto(oxidoNitrico).atributos(Map.of("presentacion", "250 g", "sabor", "Naranja")).stock(2).build());
        varianteRepository.save(Variante.builder()
                .producto(bcaa).atributos(Map.of("presentacion", "200 g", "sabor", "Limón")).stock(22).build());
        varianteRepository.save(Variante.builder()
                .producto(bcaa).atributos(Map.of("presentacion", "200 g", "sabor", "Sandía")).stock(8).build());
        varianteRepository.save(Variante.builder()
                .producto(glutamina).atributos(Map.of("presentacion", "300 g", "sabor", "Sin sabor")).stock(16).build());
        varianteRepository.save(Variante.builder()
                .producto(eaa).atributos(Map.of("presentacion", "300 g", "sabor", "Mango")).stock(5).build());
        varianteRepository.save(Variante.builder()
                .producto(multivitaminico).atributos(Map.of("presentacion", "60 comprimidos")).stock(30).build());
        varianteRepository.save(Variante.builder()
                .producto(omega3).atributos(Map.of("presentacion", "90 cápsulas")).stock(24).build());
        varianteRepository.save(Variante.builder()
                .producto(magnesio).atributos(Map.of("presentacion", "60 cápsulas")).stock(19).build());

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
                .variante(wheyChocolate)
                .producto(wheyConcentrada)
                .cantidad(1)
                .precioProducto(wheyConcentrada.getPrecio())
                .build());
        itemPedidoRepository.save(ItemPedido.builder()
                .pedido(pedido2)
                .variante(creatinaSinSabor)
                .producto(creatinaMono)
                .cantidad(3)
                .precioProducto(creatinaMono.getPrecio())
                .build());
        itemPedidoRepository.save(ItemPedido.builder()
                .pedido(pedido3)
                .variante(preFrutosRojos)
                .producto(preExplosive)
                .cantidad(2)
                .precioProducto(preExplosive.getPrecio())
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
                .precioTotal(creatinaMono.getPrecio() * 3)
                .build());
        facturaRepository.save(Factura.builder()
                .pedido(pedido3)
                .tipoFactura(TipoFactura.A)
                .precioTotal(preExplosive.getPrecio() * 2)
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
