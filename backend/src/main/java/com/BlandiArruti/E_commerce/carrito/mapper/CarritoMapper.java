package com.BlandiArruti.E_commerce.carrito.mapper;

import com.BlandiArruti.E_commerce.carrito.dto.response.CarritoResponse;
import com.BlandiArruti.E_commerce.carrito.entity.Carrito;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ItemCarritoMapper.class})
public interface CarritoMapper {

    @Mapping(source = "uuid", target = "uuidCarrito")
    @Mapping(source = "items", target = "items")
    @Mapping(target = "total", expression = "java(calcularTotal(carrito))")
    CarritoResponse toResponse(Carrito carrito);

    default Double calcularTotal(Carrito carrito) {
        return carrito.getItems().stream()
                .mapToDouble(item -> item.getProducto().getPrecio() * item.getCantidad())
                .sum();
    }
}
