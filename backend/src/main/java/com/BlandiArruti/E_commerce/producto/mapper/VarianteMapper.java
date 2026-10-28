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
    VarianteResponse toResponse(Variante variante);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "producto", ignore = true)
    Variante toEntity(VarianteRequest request);
}
