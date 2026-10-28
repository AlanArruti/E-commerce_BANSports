package com.BlandiArruti.E_commerce.administrador.mapper;

import com.BlandiArruti.E_commerce.administrador.dto.request.AdministradorRequest;
import com.BlandiArruti.E_commerce.administrador.dto.response.AdministradorResponse;
import com.BlandiArruti.E_commerce.administrador.entity.Administrador;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AdministradorMapper {

    @Mapping(source = "id", target = "idAdministrador")
    AdministradorResponse toResponse(Administrador administrador);

    @Mapping(target = "id", ignore = true)
    Administrador toEntity(AdministradorRequest request);
}
