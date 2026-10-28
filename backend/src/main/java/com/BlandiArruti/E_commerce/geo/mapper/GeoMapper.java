package com.BlandiArruti.E_commerce.geo.mapper;

import com.BlandiArruti.E_commerce.geo.dto.response.CiudadResponse;
import com.BlandiArruti.E_commerce.geo.dto.response.PaisResponse;
import com.BlandiArruti.E_commerce.geo.dto.response.ProvinciaResponse;
import com.BlandiArruti.E_commerce.geo.entity.Ciudad;
import com.BlandiArruti.E_commerce.geo.entity.Pais;
import com.BlandiArruti.E_commerce.geo.entity.Provincia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GeoMapper {

    @Mapping(source = "id", target = "idPais")
    @Mapping(source = "nombre", target = "nombrePais")
    PaisResponse toResponse(Pais pais);

    @Mapping(source = "id", target = "idProvincia")
    @Mapping(source = "nombre", target = "nombreProvincia")
    ProvinciaResponse toResponse(Provincia provincia);

    @Mapping(source = "id", target = "idCiudad")
    @Mapping(source = "nombre", target = "nombreCiudad")
    CiudadResponse toResponse(Ciudad ciudad);
}
