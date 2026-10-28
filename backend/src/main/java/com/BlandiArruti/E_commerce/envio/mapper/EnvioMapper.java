package com.BlandiArruti.E_commerce.envio.mapper;

import com.BlandiArruti.E_commerce.cliente.mapper.DireccionMapper;
import com.BlandiArruti.E_commerce.envio.dto.response.EnvioResponse;
import com.BlandiArruti.E_commerce.envio.entity.Envio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DireccionMapper.class})
public interface EnvioMapper {

    @Mapping(source = "uuid", target = "uuidEnvio")
    EnvioResponse toResponse(Envio envio);
}
