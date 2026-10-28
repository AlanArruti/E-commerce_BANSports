package com.BlandiArruti.E_commerce.cliente.mapper;

import com.BlandiArruti.E_commerce.cliente.dto.request.ClienteRequest;
import com.BlandiArruti.E_commerce.cliente.dto.response.ClienteResponse;
import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DireccionMapper.class})
public interface ClienteMapper {

    @Mapping(source = "uuid", target = "uuidCliente")
    ClienteResponse toResponse(Cliente cliente);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "direcciones", expression = "java(new java.util.ArrayList<>())")
    @Mapping(target = "pedidos", expression = "java(new java.util.ArrayList<>())")
    Cliente toEntity(ClienteRequest request);
}
