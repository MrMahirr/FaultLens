package com.faultlens.api.security;

import tools.jackson.databind.ObjectMapper;
import com.faultlens.api.dto.ErrorEnvelope;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    /** Endpoints a user with a pending password change may still call ("METHOD path"). */
    static final Set<String> PASSWORD_CHANGE_ALLOWED = Set.of(
            "PUT /api/v1/auth/password",
            "GET /api/v1/auth/profile",
            "POST /api/v1/auth/refresh");

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    /**
     * Authenticates requests that contain a valid bearer token.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                if (!jwtService.validateToken(token)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                Claims claims = jwtService.parse(token);
                if (JwtService.requiresPasswordChange(claims) && !isAllowedDuringPasswordChange(request)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(response.getWriter(), ErrorEnvelope.of(
                            "PASSWORD_CHANGE_REQUIRED", "Password must be changed before using the API"));
                    return;
                }
                Object rawRoles = claims.get("roles");
                List<SimpleGrantedAuthority> authorities = rawRoles instanceof List<?> roles
                        ? roles.stream()
                                .map(String::valueOf)
                                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                                .map(SimpleGrantedAuthority::new)
                                .toList()
                        : List.of();
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities));
            } catch (Exception ignored) {
                log.debug("JWT authentication failed");
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isAllowedDuringPasswordChange(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return PASSWORD_CHANGE_ALLOWED.contains(request.getMethod() + " " + path);
    }
}
