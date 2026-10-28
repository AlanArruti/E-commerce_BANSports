package com.BlandiArruti.E_commerce.factura.service;

import com.BlandiArruti.E_commerce.enums.TipoFactura;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import com.BlandiArruti.E_commerce.factura.dto.response.FacturaResponse;
import com.BlandiArruti.E_commerce.factura.entity.Factura;
import com.BlandiArruti.E_commerce.factura.mapper.FacturaMapper;
import com.BlandiArruti.E_commerce.factura.repository.FacturaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FacturaService implements IFacturaService {

    private final FacturaRepository facturaRepository;
    private final FacturaMapper facturaMapper;

    public List<FacturaResponse> listarTodas(TipoFactura tipo, Long idPedido, Long idCliente) {
        List<Factura> facturas;

        if (idPedido != null) {
            facturas = List.of(facturaRepository.findByPedidoId(idPedido)
                    .orElseThrow(() -> EntidadNoEncontradaException.facturaPorPedido(idPedido)));
        } else if (idCliente != null) {
            facturas = facturaRepository.findByPedidoClienteId(idCliente);
        } else if (tipo != null) {
            facturas = facturaRepository.findByTipoFactura(tipo);
        } else {
            facturas = facturaRepository.findAll();
        }

        return facturas.stream()
                .map(facturaMapper::toResponse)
                .toList();
    }

    public FacturaResponse buscarPorId(Long id) {
        return facturaMapper.toResponse(
                facturaRepository.findById(id)
                        .orElseThrow(() -> EntidadNoEncontradaException.factura(id))
        );
    }
}
