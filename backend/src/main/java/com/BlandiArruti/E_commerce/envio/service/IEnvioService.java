package com.BlandiArruti.E_commerce.envio.service;

import com.BlandiArruti.E_commerce.envio.dto.request.EnvioRequest;
import com.BlandiArruti.E_commerce.envio.dto.request.EstadoEnvioRequest;
import com.BlandiArruti.E_commerce.envio.dto.response.EnvioResponse;
import com.BlandiArruti.E_commerce.enums.EstadoEnvio;

import java.util.List;

public interface IEnvioService {
    List<EnvioResponse> listarTodos(EstadoEnvio estado);
    EnvioResponse buscarPorId(Long id);
    EnvioResponse crear(Long idPedido, EnvioRequest request);
    EnvioResponse actualizarEstado(Long id, EstadoEnvioRequest request);
    void eliminar(Long id);
}
