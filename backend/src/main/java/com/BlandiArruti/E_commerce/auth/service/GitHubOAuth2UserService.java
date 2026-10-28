package com.BlandiArruti.E_commerce.auth.service;

import com.BlandiArruti.E_commerce.cliente.entity.Cliente;
import com.BlandiArruti.E_commerce.cliente.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GitHubOAuth2UserService extends DefaultOAuth2UserService {

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(request);

        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("email_not_found"),
                "GitHub no proporcionó un email. Hacé público tu email principal en GitHub → Settings → Emails."
            );
        }

        Cliente cliente = clienteRepository.findByEmail(email)
                .orElseGet(() -> registrarDesdeGitHub(email, oAuth2User));

        return UsuarioDetails.fromClienteOAuth2(cliente, oAuth2User.getAttributes());
    }

    private Cliente registrarDesdeGitHub(String email, OAuth2User oAuth2User) {
        String nombre = oAuth2User.getAttribute("name");
        String login  = oAuth2User.getAttribute("login");

        String firstName;
        String lastName;
        if (nombre != null && !nombre.isBlank()) {
            int espacio = nombre.indexOf(' ');
            firstName = espacio > 0 ? nombre.substring(0, espacio) : nombre;
            lastName  = espacio > 0 ? nombre.substring(espacio + 1) : login;
        } else {
            firstName = login;
            lastName  = login;
        }

        return clienteRepository.save(Cliente.builder()
                .nombre(firstName)
                .apellido(lastName)
                .email(email)
                .contrasenia(passwordEncoder.encode(UUID.randomUUID().toString()))
                .build());
    }
}
