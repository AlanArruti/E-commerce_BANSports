package com.BlandiArruti.E_commerce.auth.service;

import com.BlandiArruti.E_commerce.auth.dto.request.LoginRequest;
import com.BlandiArruti.E_commerce.auth.dto.response.TokenResponse;
import com.BlandiArruti.E_commerce.auth.util.JwtUtil;
import com.BlandiArruti.E_commerce.cliente.dto.request.ClienteRequest;
import com.BlandiArruti.E_commerce.cliente.dto.response.ClienteResponse;
import com.BlandiArruti.E_commerce.cliente.service.IClienteService;
import com.BlandiArruti.E_commerce.exception.CredencialesInvalidasException;
import com.BlandiArruti.E_commerce.exception.DemasiadosIntentosException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioDetailsService usuarioDetailsService;
    private final JwtUtil jwtUtil;
    private final IClienteService clienteService;
    private final LoginAttemptService loginAttemptService;

    public TokenResponse login(LoginRequest request) {
        if (loginAttemptService.estaBloqueado(request.username())) {
            long minutos = loginAttemptService.minutosRestantes(request.username());
            throw new DemasiadosIntentosException(
                    "Cuenta temporalmente bloqueada por demasiados intentos fallidos. "
                    + "Intentá de nuevo en " + minutos + " minuto(s).");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (BadCredentialsException e) {
            loginAttemptService.loginFallido(request.username());
            throw new CredencialesInvalidasException("Credenciales inválidas.");
        } catch (DisabledException e) {
            throw new CredencialesInvalidasException("La cuenta está desactivada.");
        }

        loginAttemptService.loginExitoso(request.username());

        UsuarioDetails userDetails = (UsuarioDetails) usuarioDetailsService.loadUserByUsername(request.username());
        String token = jwtUtil.generarToken(userDetails);

        return new TokenResponse(token, "Bearer", userDetails.getRol().name(), userDetails.getId());
    }

    public ClienteResponse registrar(ClienteRequest request) {
        return clienteService.crear(request);
    }
}
