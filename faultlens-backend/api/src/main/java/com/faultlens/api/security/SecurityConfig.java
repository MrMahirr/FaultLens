package com.faultlens.api.security;

import tools.jackson.databind.ObjectMapper;
import com.faultlens.api.dto.ErrorEnvelope;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    /**
     * Creates the stateless JWT security chain.
     */
    /**
     * The actuator's own port (management.server.port), when one is configured. Prometheus
     * scrapes {@code /actuator/prometheus} there without credentials (P08); that port must only be
     * reachable from the monitoring network. On the main port the endpoint needs ADMIN.
     */
    @org.springframework.beans.factory.annotation.Value("${management.server.port:}")
    private String managementPort;

    private boolean onManagementPort(jakarta.servlet.http.HttpServletRequest request) {
        return managementPort != null && !managementPort.isBlank() && !"0".equals(managementPort.trim())
                ? Integer.toString(request.getLocalPort()).equals(managementPort.trim())
                : false;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ApiKeyAuthFilter apiKeyAuthFilter,
                                                   JwtAuthFilter jwtAuthFilter, ObjectMapper objectMapper) throws Exception {
        return http
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(response, objectMapper, HttpServletResponse.SC_FORBIDDEN, "ACCESS_DENIED", "Access denied")))
                .authorizeHttpRequests(auth -> auth
                        // WebSocket handshakes carry the JWT as a sub-protocol, not an Authorization header;
                        // they are authenticated by JwtHandshakeInterceptor (see WebSocketConfig).
                        .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/ws/**")).permitAll()
                        .requestMatchers("/api/v1/auth/**", "/actuator/health", "/livez", "/readyz", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(request -> onManagementPort(request)
                                && "/actuator/prometheus".equals(request.getRequestURI())).permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                // API keys first: a key-shaped bearer token is never treated as a JWT.
                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthFilter, ApiKeyAuthFilter.class)
                .build();
    }

    /**
     * Creates BCrypt password encoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeError(HttpServletResponse response, ObjectMapper objectMapper, int status, String code, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ErrorEnvelope.of(code, message));
    }
}
