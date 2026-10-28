package com.BlandiArruti.E_commerce.envio.repository;

import com.BlandiArruti.E_commerce.envio.entity.Envio;
import com.BlandiArruti.E_commerce.enums.EstadoEnvio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnvioRepository extends JpaRepository<Envio, Long> {

    Optional<Envio> findByUuid(String uuid);

    Optional<Envio> findByPedidoId(Long idPedido);

    List<Envio> findByEstado(EstadoEnvio estado);
}
