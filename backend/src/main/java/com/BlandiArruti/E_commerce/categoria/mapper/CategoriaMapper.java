package com.BlandiArruti.E_commerce.categoria.mapper;

import com.BlandiArruti.E_commerce.categoria.dto.request.CategoriaRequest;
import com.BlandiArruti.E_commerce.categoria.dto.response.CategoriaResponse;
import com.BlandiArruti.E_commerce.categoria.entity.Categoria;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {

    @Mapping(source = "uuid", target = "uuidCategoria")
    CategoriaResponse toResponse(Categoria categoria);

    @Mapping(target = "id", ignore = true)
    Categoria toEntity(CategoriaRequest request);
}
