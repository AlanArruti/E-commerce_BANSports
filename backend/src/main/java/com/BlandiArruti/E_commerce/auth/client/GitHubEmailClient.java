package com.BlandiArruti.E_commerce.auth.client;

import com.BlandiArruti.E_commerce.cotizacion.config.RestClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * GitHub solo devuelve el email en /user si el usuario lo tiene público.
 * Con el scope user:email se puede pedir la lista completa y elegir el principal verificado.
 */
@Component
@Slf4j
public class GitHubEmailClient {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.github.com")
            .requestFactory(RestClientConfig.conTimeouts(Duration.ofSeconds(10)))
            .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
            .build();

    record GitHubEmail(String email, boolean primary, boolean verified) {}

    public Optional<String> emailPrincipalVerificado(String accessToken) {
        try {
            List<GitHubEmail> emails = restClient.get()
                    .uri("/user/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (emails == null) {
                return Optional.empty();
            }
            return emails.stream()
                    .filter(e -> e.primary() && e.verified())
                    .map(GitHubEmail::email)
                    .findFirst();
        } catch (RestClientException e) {
            log.warn("No se pudo consultar los emails de GitHub: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
