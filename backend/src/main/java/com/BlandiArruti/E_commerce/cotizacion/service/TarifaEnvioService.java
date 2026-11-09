package com.BlandiArruti.E_commerce.cotizacion.service;

import com.BlandiArruti.E_commerce.cotizacion.dto.request.TarifaEnvioRequest;
import com.BlandiArruti.E_commerce.cotizacion.dto.response.TarifaEnvioResponse;
import com.BlandiArruti.E_commerce.cotizacion.entity.TarifaEnvio;
import com.BlandiArruti.E_commerce.cotizacion.mapper.TarifaEnvioMapper;
import com.BlandiArruti.E_commerce.cotizacion.repository.TarifaEnvioRepository;
import com.BlandiArruti.E_commerce.exception.DuplicadoException;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TarifaEnvioService implements ITarifaEnvioService {

    private final TarifaEnvioRepository tarifaEnvioRepository;
    private final TarifaEnvioMapper tarifaEnvioMapper;

    @Transactional(readOnly = true)
    public List<TarifaEnvioResponse> listarTodas() {
        return tarifaEnvioRepository.findAll().stream()
                .map(tarifaEnvioMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TarifaEnvioResponse buscarPorId(Long id) {
        return tarifaEnvioMapper.toResponse(
                tarifaEnvioRepository.findById(id)
                        .orElseThrow(() -> EntidadNoEncontradaException.tarifaEnvio(id))
        );
    }

    public TarifaEnvioResponse crear(TarifaEnvioRequest request) {
        validarNoDuplicada(request, null);

        TarifaEnvio tarifa = tarifaEnvioMapper.toEntity(request);
        return tarifaEnvioMapper.toResponse(tarifaEnvioRepository.save(tarifa));
    }

    public TarifaEnvioResponse actualizar(Long id, TarifaEnvioRequest request) {
        TarifaEnvio tarifa = tarifaEnvioRepository.findById(id)
                .orElseThrow(() -> EntidadNoEncontradaException.tarifaEnvio(id));

        validarNoDuplicada(request, id);

        tarifa.setProvincia(request.provincia());
        tarifa.setPrecio(request.precio());
        tarifa.setPlazoMinimoDias(request.plazoMinimoDias());
        tarifa.setPlazoMaximoDias(request.plazoMaximoDias());
        return tarifaEnvioMapper.toResponse(tarifaEnvioRepository.save(tarifa));
    }

    public void eliminar(Long id) {
        TarifaEnvio tarifa = tarifaEnvioRepository.findById(id)
                .orElseThrow(() -> EntidadNoEncontradaException.tarifaEnvio(id));
        tarifaEnvioRepository.delete(tarifa);
    }

    /** Una provincia solo puede tener una tarifa; el default (provincia null) es único. */
    private void validarNoDuplicada(TarifaEnvioRequest request, Long idExcluido) {
        String provincia = request.provincia() == null || request.provincia().isBlank()
                ? null
                : request.provincia().trim();

        if (provincia == null) {
            tarifaEnvioRepository.findByProvinciaIsNull().ifPresent(existente -> {
                if (!existente.getId().equals(idExcluido)) {
                    throw new DuplicadoException("Ya existe una tarifa por defecto.");
                }
            });
            return;
        }

        tarifaEnvioRepository.findByProvinciaIgnoreCase(provincia).ifPresent(existente -> {
            if (!existente.getId().equals(idExcluido)) {
                throw new DuplicadoException("Ya existe una tarifa para la provincia '" + provincia + "'.");
            }
        });
    }
}
