package com.BlandiArruti.E_commerce.auth.service;

import com.BlandiArruti.E_commerce.auth.client.GitHubEmailClient;
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

import java.util.Map;
import java.util.UUID;

/**
 * Login con GitHub y con Google (SCRUM-102, SCRUM-135).
 *
 * El usuario del proveedor se mapea a un {@link Cliente} por email: si ya hay una cuenta con ese
 * email se usa esa, si no se crea una con contraseña aleatoria (puede ponerle una con
 * "¿Olvidaste tu contraseña?").
 */
@Service
@RequiredArgsConstructor
public class OAuth2ClienteService extends DefaultOAuth2UserService {

    /** Códigos que el front traduce a mensajes (ver errores-api.ts). */
    public static final String ERROR_SIN_EMAIL = "sin_email";
    public static final String ERROR_EMAIL_NO_VERIFICADO = "email_no_verificado";
    public static final String ERROR_CUENTA_DESACTIVADA = "cuenta_desactivada";

    private static final int LARGO_NOMBRE = 50;

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final GitHubEmailClient gitHubEmailClient;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User usuario = super.loadUser(request);
        String proveedor = request.getClientRegistration().getRegistrationId();
        return procesar(proveedor, usuario.getAttributes(), request.getAccessToken().getTokenValue());
    }

    /** Separado de {@link #loadUser} para poder testearlo sin pegarle al proveedor. */
    UsuarioDetails procesar(String proveedor, Map<String, Object> atributos, String accessToken) {
        String email = switch (proveedor) {
            case "google" -> emailDeGoogle(atributos);
            case "github" -> emailDeGitHub(atributos, accessToken);
            default -> throw error("proveedor_desconocido", "Proveedor no soportado: " + proveedor);
        };

        Cliente cliente = clienteRepository.findByEmail(email)
                .orElseGet(() -> registrar(proveedor, email, atributos));

        if (!cliente.isActivo()) {
            throw error(ERROR_CUENTA_DESACTIVADA, "La cuenta está desactivada.");
        }
        return UsuarioDetails.fromClienteOAuth2(cliente, atributos);
    }

    private String emailDeGoogle(Map<String, Object> atributos) {
        String email = texto(atributos.get("email"));
        if (email == null) {
            throw error(ERROR_SIN_EMAIL, "Google no devolvió un email.");
        }
        // Sin esta verificación, alguien podría entrar a una cuenta ajena con un email no confirmado.
        if (!Boolean.TRUE.equals(atributos.get("email_verified"))) {
            throw error(ERROR_EMAIL_NO_VERIFICADO, "El email de tu cuenta de Google no está verificado.");
        }
        return email;
    }

    private String emailDeGitHub(Map<String, Object> atributos, String accessToken) {
        String email = texto(atributos.get("email"));
        if (email != null) {
            return email;
        }
        // El email está privado en GitHub: se pide la lista con el scope user:email.
        return gitHubEmailClient.emailPrincipalVerificado(accessToken)
                .orElseThrow(() -> error(ERROR_SIN_EMAIL,
                        "Tu cuenta de GitHub no tiene un email principal verificado."));
    }

    private Cliente registrar(String proveedor, String email, Map<String, Object> atributos) {
        String nombre;
        String apellido;
        if ("google".equals(proveedor)) {
            nombre = texto(atributos.get("given_name"));
            apellido = texto(atributos.get("family_name"));
        } else {
            String completo = texto(atributos.get("name"));
            nombre = completo;
            apellido = null;
            if (completo != null && completo.contains(" ")) {
                int espacio = completo.indexOf(' ');
                nombre = completo.substring(0, espacio);
                apellido = completo.substring(espacio + 1).trim();
            }
            if (nombre == null) {
                nombre = texto(atributos.get("login"));
            }
        }
        if (nombre == null) {
            nombre = email.substring(0, email.indexOf('@'));
        }

        return clienteRepository.save(Cliente.builder()
                .nombre(recortar(nombre))
                // El apellido es obligatorio en la base; se puede completar después desde el perfil.
                .apellido(apellido == null ? "-" : recortar(apellido))
                .email(email)
                .contrasenia(passwordEncoder.encode(UUID.randomUUID().toString()))
                .build());
    }

    private static String texto(Object valor) {
        return valor instanceof String s && !s.isBlank() ? s.trim() : null;
    }

    private static String recortar(String valor) {
        return valor.length() > LARGO_NOMBRE ? valor.substring(0, LARGO_NOMBRE) : valor;
    }

    private static OAuth2AuthenticationException error(String codigo, String mensaje) {
        return new OAuth2AuthenticationException(new OAuth2Error(codigo, mensaje, null), mensaje);
    }
}
