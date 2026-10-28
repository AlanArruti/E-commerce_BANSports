package com.BlandiArruti.E_commerce.pedido.mapper;

import com.BlandiArruti.E_commerce.pedido.dto.response.ItemPedidoResponse;
import com.BlandiArruti.E_commerce.pedido.entity.ItemPedido;
import com.BlandiArruti.E_commerce.producto.mapper.ProductoMapper;
import com.BlandiArruti.E_commerce.producto.mapper.VarianteMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ProductoMapper.class, VarianteMapper.class})
public interface ItemPedidoMapper {

    @Mapping(source = "id", target = "idItemPedido")
    @Mapping(source = "precioProducto", target = "precioTotalProducto")
    ItemPedidoResponse toResponse(ItemPedido item);
}
