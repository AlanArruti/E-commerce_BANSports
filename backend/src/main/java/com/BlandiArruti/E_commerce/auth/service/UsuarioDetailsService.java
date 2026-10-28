package com.BlandiArruti.E_commerce.auth.service;

import com.BlandiArruti.E_commerce.administrador.entity.Administrador;
import com.BlandiArruti.E_commerce.administrador.repository.AdministradorRepository;
import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final ClienteRepository clienteRepository;
    private final AdministradorRepository administradorRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        Optional<Cliente> cliente = clienteRepository.findByEmail(identifier);
        if (cliente.isPresent()) {
            return UsuarioDetails.fromCliente(cliente.get());
        }

        Optional<Administrador> admin = administradorRepository.findByUsername(identifier);
        if (admin.isPresent()) {
            return UsuarioDetails.fromAdmin(admin.get());
        }

        throw new UsernameNotFoundException("Usuario no encontrado: " + identifier);
    }
}
