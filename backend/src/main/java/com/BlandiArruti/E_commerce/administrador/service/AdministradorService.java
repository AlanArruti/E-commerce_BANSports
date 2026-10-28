package com.BlandiArruti.E_commerce.administrador.service;

import com.BlandiArruti.E_commerce.administrador.dto.request.AdministradorRequest;
import com.BlandiArruti.E_commerce.administrador.dto.response.AdministradorResponse;
import com.BlandiArruti.E_commerce.administrador.entity.Administrador;
import com.BlandiArruti.E_commerce.administrador.mapper.AdministradorMapper;
import com.BlandiArruti.E_commerce.administrador.repository.AdministradorRepository;
import com.BlandiArruti.E_commerce.exception.DuplicadoException;
import com.BlandiArruti.E_commerce.exception.EntidadNoEncontradaException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AdministradorService implements IAdministradorService {

    private final AdministradorRepository administradorRepository;
    private final AdministradorMapper administradorMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<AdministradorResponse> listarTodos() {
        return administradorRepository.findAll().stream()
                .map(administradorMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdministradorResponse buscarPorId(Long id) {
        return administradorMapper.toResponse(
                administradorRepository.findById(id)
                        .orElseThrow(() -> EntidadNoEncontradaException.administrador(id))
        );
    }

    public AdministradorResponse crear(AdministradorRequest request) {
        if (administradorRepository.findByUsername(request.username()).isPresent()) {
            throw DuplicadoException.username(request.username());
        }
        Administrador admin = administradorMapper.toEntity(request);
        admin.setContrasenia(passwordEncoder.encode(request.contrasenia()));
        return administradorMapper.toResponse(administradorRepository.save(admin));
    }

    public AdministradorResponse actualizar(Long id, AdministradorRequest request) {
        Administrador admin = administradorRepository.findById(id)
                .orElseThrow(() -> EntidadNoEncontradaException.administrador(id));

        administradorRepository.findByUsername(request.username())
                .filter(a -> !a.getId().equals(id))
                .ifPresent(a -> { throw DuplicadoException.username(request.username()); });

        admin.setUsername(request.username());
        admin.setContrasenia(passwordEncoder.encode(request.contrasenia()));
        return administradorMapper.toResponse(administradorRepository.save(admin));
    }

    public void eliminar(Long id) {
        Administrador admin = administradorRepository.findById(id)
                .orElseThrow(() -> EntidadNoEncontradaException.administrador(id));
        administradorRepository.delete(admin);
    }
}
