package com.BlandiArruti.E_commerce.factura.service;

import com.BlandiArruti.E_commerce.enums.TipoFactura;
import com.BlandiArruti.E_commerce.factura.dto.response.FacturaResponse;

import java.util.List;

public interface IFacturaService {
    List<FacturaResponse> listarTodas(TipoFactura tipo, Long idPedido, Long idCliente);
    FacturaResponse buscarPorId(Long id);
}
