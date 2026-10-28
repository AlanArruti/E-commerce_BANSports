package com.BlandiArruti.E_commerce.carrito.mapper;

import com.BlandiArruti.E_commerce.carrito.dto.response.ItemCarritoResponse;
import com.BlandiArruti.E_commerce.carrito.entity.ItemCarrito;
import com.BlandiArruti.E_commerce.producto.mapper.ProductoMapper;
import com.BlandiArruti.E_commerce.producto.mapper.VarianteMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ProductoMapper.class, VarianteMapper.class})
public interface ItemCarritoMapper {

    @Mapping(source = "id", target = "idItemCarrito")
    @Mapping(target = "subtotal", expression = "java(item.getProducto().getPrecio() * item.getCantidad())")
    ItemCarritoResponse toResponse(ItemCarrito item);
}
