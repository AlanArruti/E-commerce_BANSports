package com.BlandiArruti.E_commerce.pago.service;

import com.BlandiArruti.E_commerce.enums.EstadoPago;
import com.BlandiArruti.E_commerce.pago.entity.Pago;
import com.BlandiArruti.E_commerce.pago.pasarela.RegistroPasarelas;
import com.BlandiArruti.E_commerce.pago.repository.PagoRepository;
import com.BlandiArruti.E_commerce.pedido.entity.Pedido;
import com.BlandiArruti.E_commerce.pedido.service.IReembolsoPagos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Devuelve los cobros de un pedido que se cancela ya pagado (RF04). Habla con cada pasarela a
 * través de {@link com.BlandiArruti.E_commerce.pago.pasarela.PasarelaPago}, sin saber cuál es.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReembolsoPagosService implements IReembolsoPagos {

    private final PagoRepository pagoRepository;
    private final RegistroPasarelas registroPasarelas;

    @Override
    public double reembolsarCobros(Pedido pedido) {
        List<Pago> aprobados = pagoRepository.findByPedidoIdAndEstado(pedido.getId(), EstadoPago.APROBADO);
        double total = 0;
        for (Pago pago : aprobados) {
            // Si la pasarela lo rechaza, lanza acá: la transacción se deshace y el pedido sigue como estaba.
            registroPasarelas.obtener(pago.getPasarela()).reembolsar(pago.getIdExterno());
            pago.setEstado(EstadoPago.REEMBOLSADO);
            pagoRepository.save(pago);
            total += pago.getMontoPagado() != null ? pago.getMontoPagado() : pago.getMonto();
            log.info("Pago {} ({}) del pedido {} devuelto al cliente.", pago.getId(), pago.getPasarela(), pedido.getId());
        }
        return total;
    }
}
