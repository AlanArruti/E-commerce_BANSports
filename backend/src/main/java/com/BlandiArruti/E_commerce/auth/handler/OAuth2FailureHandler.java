package com.BlandiArruti.E_commerce.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Set;

/**
 * Si el login con GitHub o Google falla (o el usuario lo cancela), vuelve al login del front
 * con un código de error que el front traduce a un mensaje. Sin front configurado responde JSON.
 */
@Component
@Slf4j
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    /** Códigos que el front sabe mostrar. Cualquier otro se manda como "fallo". */
    private static final Set<String> CODIGOS_CONOCIDOS =
            Set.of("access_denied", "sin_email", "email_no_verificado", "cuenta_desactivada");

    @Value("${app.oauth2.redirect-uri:}")
    private String redirectUri;

    @Value("${app.front-url:http://localhost:4200}")
    private String frontUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String codigo = exception instanceof OAuth2AuthenticationException oauth
                ? oauth.getError().getErrorCode()
                : "fallo";
        log.warn("Login social fallido ({}): {}", codigo, exception.getMessage());

        if (redirectUri == null || redirectUri.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"status\":401,\"error\":\"OAuth2 fallido\",\"message\":\"" + codigo + "\"}");
            return;
        }

        String destino = UriComponentsBuilder.fromUriString(quitarBarraFinal(frontUrl) + "/login")
                .queryParam("errorSocial", CODIGOS_CONOCIDOS.contains(codigo) ? codigo : "fallo")
                .build()
                .toUriString();
        getRedirectStrategy().sendRedirect(request, response, destino);
    }

    private static String quitarBarraFinal(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
