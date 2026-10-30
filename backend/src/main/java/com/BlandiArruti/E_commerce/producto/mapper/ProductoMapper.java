package com.BlandiArruti.E_commerce.producto.mapper;

import com.BlandiArruti.E_commerce.categoria.mapper.CategoriaMapper;
import com.BlandiArruti.E_commerce.producto.dto.request.ProductoRequest;
import com.BlandiArruti.E_commerce.producto.dto.response.ProductoResponse;
import com.BlandiArruti.E_commerce.producto.entity.Producto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CategoriaMapper.class, VarianteMapper.class})
public interface ProductoMapper {

    @Mapping(source = "uuid", target = "uuidProducto")
    @Mapping(source = "precio", target = "precioBase")
    @Mapping(target = "precioFinal", expression = "java(producto.getPrecioConDescuento())")
    @Mapping(target = "imagenUrl", expression = "java(producto.isTieneImagen() "
            + "? \"/api/v1/producto/\" + producto.getId() + \"/imagen\" : null)")
    ProductoResponse toResponse(Producto producto);

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "precioBase", target = "precio")
    @Mapping(target = "precioFinal", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(target = "tieneImagen", ignore = true)
    @Mapping(target = "variantes", expression = "java(new java.util.ArrayList<>())")
    Producto toEntity(ProductoRequest request);
}
