package com.BlandiArruti.E_commerce.cotizacion.mapper;

import com.BlandiArruti.E_commerce.cotizacion.dto.request.TarifaEnvioRequest;
import com.BlandiArruti.E_commerce.cotizacion.dto.response.TarifaEnvioResponse;
import com.BlandiArruti.E_commerce.cotizacion.entity.TarifaEnvio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TarifaEnvioMapper {

    @Mapping(source = "uuid", target = "uuidTarifa")
    TarifaEnvioResponse toResponse(TarifaEnvio tarifa);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    TarifaEnvio toEntity(TarifaEnvioRequest request);
}
