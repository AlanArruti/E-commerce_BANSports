package com.BlandiArruti.E_commerce.auth.controller;

import com.BlandiArruti.E_commerce.auth.dto.request.LoginRequest;
import com.BlandiArruti.E_commerce.auth.dto.response.TokenResponse;
import com.BlandiArruti.E_commerce.auth.dto.response.UsuarioActualResponse;
import com.BlandiArruti.E_commerce.auth.service.AuthService;
import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.cliente.dto.request.ClienteRequest;
import com.BlandiArruti.E_commerce.cliente.dto.response.ClienteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticación", description = "Login y registro de usuarios")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Iniciar sesión", description = "Devuelve un JWT Bearer token válido por 24 horas.")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Registrar nuevo cliente", description = "No requiere autenticación.")
    @PostMapping("/register")
    public ResponseEntity<ClienteResponse> registrar(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @Operation(summary = "Usuario autenticado",
               description = "Devuelve id, username y rol del dueño del token. Sirve para "
                           + "que el front valide la sesión al arrancar.")
    @GetMapping("/me")
    public ResponseEntity<UsuarioActualResponse> usuarioActual(
            @AuthenticationPrincipal UsuarioDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new UsuarioActualResponse(
                principal.getId(), principal.getUsername(), principal.getRol().name()));
    }
}
