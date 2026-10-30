package com.BlandiArruti.E_commerce.producto.service;

import com.BlandiArruti.E_commerce.producto.dto.request.ProductoRequest;
import com.BlandiArruti.E_commerce.producto.dto.request.StockRequest;
import com.BlandiArruti.E_commerce.producto.dto.request.VarianteRequest;
import com.BlandiArruti.E_commerce.producto.dto.response.ImagenProductoResponse;
import com.BlandiArruti.E_commerce.producto.dto.response.ProductoResponse;
import com.BlandiArruti.E_commerce.producto.dto.response.VarianteResponse;
import com.BlandiArruti.E_commerce.shared.dto.PageResponse;
import com.BlandiArruti.E_commerce.producto.entity.ImagenProducto;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IProductoService {
    PageResponse<ProductoResponse> listarTodos(Long categoriaId, Double precioMin, Double precioMax,
                                               String search, Boolean soloOfertas, Pageable pageable);
    ProductoResponse buscarPorId(Long id);
    ProductoResponse crear(ProductoRequest request);
    ProductoResponse actualizar(Long id, ProductoRequest request);
    void eliminar(Long id);
    List<VarianteResponse> listarVariantes(Long idProducto);
    VarianteResponse agregarVariante(Long idProducto, VarianteRequest request);
    VarianteResponse actualizarVariante(Long idProducto, Long idVariante, VarianteRequest request);
    void eliminarVariante(Long idProducto, Long idVariante);
    VarianteResponse ajustarStock(Long idProducto, Long idVariante, StockRequest request);
    ImagenProductoResponse subirImagen(Long idProducto, MultipartFile archivo);
    ImagenProducto obtenerImagen(Long idProducto);
    void eliminarImagen(Long idProducto);
}
