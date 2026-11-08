package com.BlandiArruti.E_commerce.envio.mapper;

import com.BlandiArruti.E_commerce.cliente.mapper.DireccionMapper;
import com.BlandiArruti.E_commerce.envio.dto.response.EnvioResponse;
import com.BlandiArruti.E_commerce.envio.entity.Envio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DireccionMapper.class})
public interface EnvioMapper {

    @Mapping(source = "uuid", target = "uuidEnvio")
    @Mapping(source = "pedido.id", target = "idPedido")
    @Mapping(target = "cliente", expression = "java(nombreCliente(envio))")
    EnvioResponse toResponse(Envio envio);

    default String nombreCliente(Envio envio) {
        if (envio.getPedido() == null || envio.getPedido().getCliente() == null) {
            return null;
        }
        var cliente = envio.getPedido().getCliente();
        return cliente.getNombre() + " " + cliente.getApellido();
    }
}
