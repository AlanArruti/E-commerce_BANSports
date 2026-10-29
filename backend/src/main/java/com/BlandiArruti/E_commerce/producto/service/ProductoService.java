package com.BlandiArruti.E_commerce.producto.service;

import com.BlandiArruti.E_commerce.categoria.entity.Categoria;
import com.BlandiArruti.E_commerce.categoria.repository.CategoriaRepository;
import com.BlandiArruti.E_commerce.enums.OperacionStock;
import com.BlandiArruti.E_commerce.exception.EcommerceException;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.exception.StockInsuficienteException;
import com.BlandiArruti.E_commerce.producto.dto.request.ProductoRequest;
import com.BlandiArruti.E_commerce.producto.dto.request.StockRequest;
import com.BlandiArruti.E_commerce.producto.dto.request.VarianteRequest;
import com.BlandiArruti.E_commerce.producto.dto.response.ImagenProductoResponse;
import com.BlandiArruti.E_commerce.producto.dto.response.ProductoResponse;
import com.BlandiArruti.E_commerce.producto.dto.response.VarianteResponse;
import com.BlandiArruti.E_commerce.producto.entity.ImagenProducto;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import com.BlandiArruti.E_commerce.producto.entity.Variante;
import com.BlandiArruti.E_commerce.producto.mapper.ProductoMapper;
import com.BlandiArruti.E_commerce.producto.mapper.VarianteMapper;
import com.BlandiArruti.E_commerce.producto.repository.ImagenProductoRepository;
import com.BlandiArruti.E_commerce.producto.repository.ProductoRepository;
import com.BlandiArruti.E_commerce.producto.repository.VarianteRepository;
import com.BlandiArruti.E_commerce.shared.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductoService implements IProductoService {

    /** Formatos de imagen aceptados para el catálogo. */
    private static final Set<String> TIPOS_IMAGEN_PERMITIDOS =
            Set.of("image/jpeg", "image/png", "image/webp");

    /** Tope por imagen: 2 MB. */
    private static final long TAMANIO_MAXIMO_IMAGEN = 2L * 1024 * 1024;

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final VarianteRepository varianteRepository;
    private final ImagenProductoRepository imagenProductoRepository;
    private final ProductoMapper productoMapper;
    private final VarianteMapper varianteMapper;

    public ImagenProductoResponse subirImagen(Long idProducto, MultipartFile archivo) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> EntidadNoEncontradaException.producto(idProducto));

        if (archivo == null || archivo.isEmpty()) {
            throw new EcommerceException("El archivo de imagen está vacío.");
        }
        if (archivo.getSize() > TAMANIO_MAXIMO_IMAGEN) {
            throw new EcommerceException("La imagen no puede superar los 2 MB.");
        }
        String contentType = archivo.getContentType();
        if (contentType == null || !TIPOS_IMAGEN_PERMITIDOS.contains(contentType.toLowerCase())) {
            throw new EcommerceException(
                    "Formato de imagen no soportado. Se aceptan: JPEG, PNG y WEBP.");
        }

        byte[] datos;
        try {
            datos = archivo.getBytes();
        } catch (IOException e) {
            throw new EcommerceException("No se pudo leer el archivo de imagen.");
        }

        // Una imagen por producto: si ya había, se reemplaza.
        ImagenProducto imagen = imagenProductoRepository.findByProductoId(idProducto)
                .orElseGet(() -> ImagenProducto.builder().producto(producto).build());
        imagen.setNombreArchivo(archivo.getOriginalFilename() != null
                ? archivo.getOriginalFilename() : "imagen");
        imagen.setContentType(contentType.toLowerCase());
        imagen.setDatos(datos);
        imagenProductoRepository.save(imagen);

        producto.setTieneImagen(true);
        productoRepository.save(producto);

        return new ImagenProductoResponse(
                idProducto,
                imagen.getNombreArchivo(),
                imagen.getContentType(),
                datos.length,
                "/api/v1/producto/" + idProducto + "/imagen");
    }

    @Transactional(readOnly = true)
    public ImagenProducto obtenerImagen(Long idProducto) {
        return imagenProductoRepository.findByProductoId(idProducto)
                .orElseThrow(() -> new EntidadNoEncontradaException(
                        "El producto con id " + idProducto + " no tiene imagen cargada."));
    }

    public void eliminarImagen(Long idProducto) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> EntidadNoEncontradaException.producto(idProducto));

        ImagenProducto imagen = imagenProductoRepository.findByProductoId(idProducto)
                .orElseThrow(() -> new EntidadNoEncontradaException(
                        "El producto con id " + idProducto + " no tiene imagen cargada."));

        imagenProductoRepository.delete(imagen);
        producto.setTieneImagen(false);
        productoRepository.save(producto);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductoResponse> listarTodos(Long categoriaId, Double precioMin, Double precioMax, String search, Pageable pageable) {
        String searchParam = normalizarBusqueda(search);
        return PageResponse.from(
                productoRepository.buscarConFiltros(categoriaId, precioMin, precioMax, searchParam, pageable)
                        .map(productoMapper::toResponse)
        );
    }

    // Mismo criterio que ProductoRepository.SIN_TILDES_INICIO: minúsculas y sin tildes (la ñ queda).
    // Además cada palabra pasa a singular sacándole la "s" final: como la búsqueda es por
    // "contiene", "proteina" encuentra tanto "Proteína Vegana" como la categoría "Proteínas".
    private static String normalizarBusqueda(String search) {
        if (search == null || search.isBlank()) {
            return "";
        }
        String sinTildes = search.trim().toLowerCase()
                .replace('á', 'a').replace('é', 'e').replace('í', 'i')
                .replace('ó', 'o').replace('ú', 'u').replace('ü', 'u');
        return Arrays.stream(sinTildes.split("\\s+"))
                .map(ProductoService::singular)
                .collect(Collectors.joining(" "));
    }

    // Las palabras cortas quedan como están ("gas", "mas"): ahí la "s" no suele ser plural.
    private static String singular(String palabra) {
        return palabra.length() > 3 && palabra.endsWith("s")
                ? palabra.substring(0, palabra.length() - 1)
                : palabra;
    }

    @Transactional(readOnly = true)
    public ProductoResponse buscarPorId(Long id) {
        return productoMapper.toResponse(
                productoRepository.findByIdAndActivoTrue(id)
                        .orElseThrow(() -> EntidadNoEncontradaException.producto(id))
        );
    }

    public ProductoResponse crear(ProductoRequest request) {
        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> EntidadNoEncontradaException.categoria(request.categoriaId()));

        Producto producto = productoMapper.toEntity(request);
        producto.setCategoria(categoria);
        return productoMapper.toResponse(productoRepository.save(producto));
    }

    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = productoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> EntidadNoEncontradaException.producto(id));

        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> EntidadNoEncontradaException.categoria(request.categoriaId()));

        producto.setNombre(request.nombre());
        producto.setDescripcion(request.descripcion());
        producto.setPrecio(request.precioBase());
        producto.setCategoria(categoria);
        return productoMapper.toResponse(productoRepository.save(producto));
    }

    public void eliminar(Long id) {
        Producto producto = productoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> EntidadNoEncontradaException.producto(id));
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    @Transactional(readOnly = true)
    public List<VarianteResponse> listarVariantes(Long idProducto) {
        if (productoRepository.findByIdAndActivoTrue(idProducto).isEmpty()) {
            throw EntidadNoEncontradaException.producto(idProducto);
        }
        return varianteRepository.findByProductoIdAndActivoTrue(idProducto).stream()
                .map(varianteMapper::toResponse)
                .toList();
    }

    public VarianteResponse agregarVariante(Long idProducto, VarianteRequest request) {
        Producto producto = productoRepository.findByIdAndActivoTrue(idProducto)
                .orElseThrow(() -> EntidadNoEncontradaException.producto(idProducto));

        Variante variante = varianteMapper.toEntity(request);
        variante.setProducto(producto);
        return varianteMapper.toResponse(varianteRepository.save(variante));
    }

    public VarianteResponse actualizarVariante(Long idProducto, Long idVariante, VarianteRequest request) {
        productoRepository.findByIdAndActivoTrue(idProducto)
                .orElseThrow(() -> EntidadNoEncontradaException.producto(idProducto));
        Variante variante = varianteRepository.findByIdAndActivoTrue(idVariante)
                .filter(v -> v.getProducto().getId().equals(idProducto))
                .orElseThrow(() -> EntidadNoEncontradaException.variante(idVariante));

        variante.setAtributos(request.atributos());
        variante.setStock(request.stock());
        return varianteMapper.toResponse(varianteRepository.save(variante));
    }

    public void eliminarVariante(Long idProducto, Long idVariante) {
        productoRepository.findByIdAndActivoTrue(idProducto)
                .orElseThrow(() -> EntidadNoEncontradaException.producto(idProducto));
        Variante variante = varianteRepository.findByIdAndActivoTrue(idVariante)
                .filter(v -> v.getProducto().getId().equals(idProducto))
                .orElseThrow(() -> EntidadNoEncontradaException.variante(idVariante));
        variante.setActivo(false);
        varianteRepository.save(variante);
    }

    public VarianteResponse ajustarStock(Long idProducto, Long idVariante, StockRequest request) {
        productoRepository.findByIdAndActivoTrue(idProducto)
                .orElseThrow(() -> EntidadNoEncontradaException.producto(idProducto));
        Variante variante = varianteRepository.findByIdAndActivoTrue(idVariante)
                .filter(v -> v.getProducto().getId().equals(idProducto))
                .orElseThrow(() -> EntidadNoEncontradaException.variante(idVariante));

        if (request.operacion() == OperacionStock.AGREGAR) {
            variante.setStock(variante.getStock() + request.cantidad());
        } else {
            if (variante.getStock() < request.cantidad()) {
                throw StockInsuficienteException.stockInsuficiente(
                        idVariante, variante.getStock(), request.cantidad());
            }
            variante.setStock(variante.getStock() - request.cantidad());
        }

        return varianteMapper.toResponse(varianteRepository.save(variante));
    }
}
