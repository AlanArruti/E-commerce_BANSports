package com.BlandiArruti.E_commerce.auth.handler;

import com.BlandiArruti.E_commerce.auth.service.UsuarioDetails;
import com.BlandiArruti.E_commerce.auth.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Al completarse el login con OAuth2 genera el JWT propio y redirige al front
 * con el token en la query string, para que el SPA lo guarde y siga navegando.
 * Si no hay front configurado, devuelve el token como JSON (útil para Postman).
 */
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    @Value("${app.oauth2.redirect-uri:}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        UsuarioDetails userDetails = (UsuarioDetails) authentication.getPrincipal();
        String token = jwtUtil.generarToken(userDetails);

        if (redirectUri == null || redirectUri.isBlank()) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(String.format(
                    "{\"token\":\"%s\",\"tipo\":\"Bearer\",\"rol\":\"%s\",\"id\":%d}",
                    token, userDetails.getRol().name(), userDetails.getId()
            ));
            return;
        }

        String destino = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", token)
                .queryParam("rol", userDetails.getRol().name())
                .queryParam("id", userDetails.getId())
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, destino);
    }
}
