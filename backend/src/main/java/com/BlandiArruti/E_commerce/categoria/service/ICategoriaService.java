package com.BlandiArruti.E_commerce.categoria.service;

import com.BlandiArruti.E_commerce.categoria.dto.request.CategoriaRequest;
import com.BlandiArruti.E_commerce.categoria.dto.response.CategoriaResponse;
import com.BlandiArruti.E_commerce.categoria.dto.response.EliminacionResponse;

import java.util.List;

public interface ICategoriaService {
    List<CategoriaResponse> listarTodas(String nombre);
    CategoriaResponse buscarPorId(Long id);
    CategoriaResponse crear(CategoriaRequest request);
    CategoriaResponse actualizar(Long id, CategoriaRequest request);
    EliminacionResponse eliminar(Long id, boolean confirmar);
}
