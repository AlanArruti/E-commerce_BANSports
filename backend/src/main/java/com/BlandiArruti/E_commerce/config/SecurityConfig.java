package com.BlandiArruti.E_commerce.config;

import com.BlandiArruti.E_commerce.auth.filter.JwtAuthFilter;
import com.BlandiArruti.E_commerce.auth.handler.OAuth2SuccessHandler;
import com.BlandiArruti.E_commerce.auth.service.GitHubOAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final GitHubOAuth2UserService gitHubOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    /** Orígenes del front autorizados a consumir la API (separados por coma). */
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://localhost:4200}")
    private String origenesPermitidos;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)

            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        "{\"status\":401,\"error\":\"No autorizado\",\"message\":\"" +
                        escaparJson(authException.getMessage()) + "\"}"
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        "{\"status\":403,\"error\":\"Acceso denegado\",\"message\":\"" +
                        escaparJson(accessDeniedException.getMessage()) + "\"}"
                    );
                })
            )

            .authorizeHttpRequests(auth -> auth

                // ── Públicos ────────────────────────────────────────────────
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()

                // Healthcheck de Docker/orquestador. Solo /health: el resto de
                // Actuator queda cerrado (ver management.endpoints en el yaml).
                .requestMatchers("/actuator/health/**").permitAll()

                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/webhook/**").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

                .requestMatchers(HttpMethod.GET, "/api/v1/producto/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/categorias/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/paises/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/provincias/**").permitAll()

                // ── Solo ADMIN ───────────────────────────────────────────────
                .requestMatchers("/api/v1/administrador/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/envio/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/factura/**").hasRole("ADMIN")

                .requestMatchers(HttpMethod.POST,   "/api/v1/producto/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/producto/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/producto/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/api/v1/producto/**").hasRole("ADMIN")

                .requestMatchers(HttpMethod.POST,   "/api/v1/categorias/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/categorias/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/categorias/**").hasRole("ADMIN")

                // ── ADMIN o CLIENTE ──────────────────────────────────────────
                .requestMatchers("/api/v1/pedido/**").hasAnyRole("ADMIN", "CLIENTE")
                .requestMatchers("/api/v1/cliente/**").hasAnyRole("ADMIN", "CLIENTE")
                .requestMatchers("/api/v1/cotizacion/**").hasAnyRole("ADMIN", "CLIENTE")

                .anyRequest().authenticated()
            )

            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(info -> info.userService(gitHubOAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
                .failureHandler((req, res, ex) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write(
                        "{\"status\":401,\"error\":\"OAuth2 fallido\",\"message\":\"" + escaparJson(ex.getMessage()) + "\"}"
                    );
                })
            )

            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.asList(origenesPermitidos.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static String escaparJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
