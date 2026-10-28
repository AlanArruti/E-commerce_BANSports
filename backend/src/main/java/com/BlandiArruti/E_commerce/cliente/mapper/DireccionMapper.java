package com.BlandiArruti.E_commerce.cliente.mapper;

import com.BlandiArruti.E_commerce.cliente.dto.response.DireccionResponse;
import com.BlandiArruti.E_commerce.cliente.entity.Direccion;
import com.BlandiArruti.E_commerce.geo.mapper.GeoMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {GeoMapper.class})
public interface DireccionMapper {

    @Mapping(source = "uuid", target = "uuidDireccion")
    DireccionResponse toResponse(Direccion direccion);
}
