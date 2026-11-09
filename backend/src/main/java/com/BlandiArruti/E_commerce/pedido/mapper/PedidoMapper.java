package com.BlandiArruti.E_commerce.pedido.mapper;

import com.BlandiArruti.E_commerce.cliente.mapper.ClienteMapper;
import com.BlandiArruti.E_commerce.envio.mapper.EnvioMapper;
import com.BlandiArruti.E_commerce.factura.mapper.FacturaMapper;
import com.BlandiArruti.E_commerce.pedido.dto.response.PedidoResponse;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ClienteMapper.class, ItemPedidoMapper.class, EnvioMapper.class, FacturaMapper.class})
public interface PedidoMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "uuid", target = "uuidPedido")
    @Mapping(source = "estadoPedido", target = "estado")
    @Mapping(source = "items", target = "items")
    @Mapping(source = "costoEnvio", target = "costoEnvio")
    @Mapping(source = "descuentoCupon", target = "descuentoCupon")
    @Mapping(source = "cupon.codigo", target = "codigoCupon")
    @Mapping(target = "total", expression = "java(calcularTotal(pedido))")
    PedidoResponse toResponse(Pedido pedido);

    /** Igual que PedidoService.calcularTotal: productos menos el cupón, más el envío. */
    default Double calcularTotal(Pedido pedido) {
        double costoEnvio = pedido.getCostoEnvio() != null ? pedido.getCostoEnvio() : 0.0;
        double descuento = pedido.getDescuentoCupon() != null ? pedido.getDescuentoCupon() : 0.0;
        if (pedido.getItems() == null) {
            return costoEnvio;
        }
        return pedido.getItems().stream()
                .mapToDouble(item -> item.getPrecioProducto() != null ? item.getPrecioProducto() : 0.0)
                .sum() - descuento + costoEnvio;
    }
}
