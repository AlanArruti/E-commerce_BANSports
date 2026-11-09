package com.BlandiArruti.E_commerce.cotizacion.service;

import com.BlandiArruti.E_commerce.cotizacion.dto.request.TarifaEnvioRequest;
import com.BlandiArruti.E_commerce.cotizacion.dto.response.TarifaEnvioResponse;

import java.util.List;

public interface ITarifaEnvioService {
    List<TarifaEnvioResponse> listarTodas();
    TarifaEnvioResponse buscarPorId(Long id);
    TarifaEnvioResponse crear(TarifaEnvioRequest request);
    TarifaEnvioResponse actualizar(Long id, TarifaEnvioRequest request);
    void eliminar(Long id);
}
