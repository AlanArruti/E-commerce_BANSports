package com.BlandiArruti.E_commerce.producto.mapper;

import com.BlandiArruti.E_commerce.producto.dto.request.VarianteRequest;
import com.BlandiArruti.E_commerce.producto.dto.response.VarianteResponse;
import com.BlandiArruti.E_commerce.producto.entity.Variante;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VarianteMapper {

    @Mapping(source = "uuid", target = "uuidVariante")
    @Mapping(source = "producto.id", target = "idProducto")
    @Mapping(target = "precioFinal",
             expression = "java(variante.getProducto() == null ? null : variante.getProducto().precioUnitario(variante))")
    @Mapping(target = "imagenUrl", expression = "java(variante.isTieneImagen() && variante.getProducto() != null "
            + "? \"/api/v1/producto/\" + variante.getProducto().getId() + \"/variante/\" + variante.getId() + \"/imagen\" : null)")
    VarianteResponse toResponse(Variante variante);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "producto", ignore = true)
    @Mapping(target = "tieneImagen", ignore = true)
    Variante toEntity(VarianteRequest request);
}
