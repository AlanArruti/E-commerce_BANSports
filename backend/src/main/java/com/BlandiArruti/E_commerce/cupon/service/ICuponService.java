package com.BlandiArruti.E_commerce.cupon.service;

import com.BlandiArruti.E_commerce.cupon.dto.request.CuponRequest;
import com.BlandiArruti.E_commerce.cupon.dto.response.CuponResponse;
import com.BlandiArruti.E_commerce.cupon.dto.response.ValidacionCuponResponse;
import com.BlandiArruti.E_commerce.cupon.entity.Cupon;

import java.util.List;

public interface ICuponService {

    // ── Panel (solo ADMIN) ─────────────────────────────────
    List<CuponResponse> listar();
    CuponResponse buscarPorId(Long id);
    CuponResponse crear(CuponRequest request);
    CuponResponse actualizar(Long id, CuponRequest request);
    /** Baja lógica: deja de aceptarse, los pedidos que lo usaron no cambian. */
    void eliminar(Long id);

    // ── Checkout ───────────────────────────────────────────
    /** Vista previa para el carrito del cliente logueado. No consume usos. */
    ValidacionCuponResponse validarParaCarrito(String codigo);

    /**
     * Valida el cupón para ese subtotal y le suma un uso. Lo llama PedidoService al crear el
     * pedido, dentro de su transacción: si el pedido falla, el uso se revierte.
     */
    CuponAplicado aplicar(String codigo, double subtotal);

    /** El pedido que lo usó se canceló: el uso vuelve a estar disponible. */
    void devolverUso(Cupon cupon);

    /** Cupón aplicado a un pedido y cuánto descontó. */
    record CuponAplicado(Cupon cupon, double descuento) {}
}
