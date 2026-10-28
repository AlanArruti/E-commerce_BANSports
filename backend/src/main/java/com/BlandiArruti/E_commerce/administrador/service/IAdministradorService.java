package com.BlandiArruti.E_commerce.administrador.service;

import com.BlandiArruti.E_commerce.administrador.dto.request.AdministradorRequest;
import com.BlandiArruti.E_commerce.administrador.dto.response.AdministradorResponse;

import java.util.List;

public interface IAdministradorService {
    List<AdministradorResponse> listarTodos();
    AdministradorResponse buscarPorId(Long id);
    AdministradorResponse crear(AdministradorRequest request);
    AdministradorResponse actualizar(Long id, AdministradorRequest request);
    void eliminar(Long id);
}
