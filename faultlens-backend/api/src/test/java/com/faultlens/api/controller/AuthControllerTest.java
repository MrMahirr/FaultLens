package com.faultlens.api.controller;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.faultlens.api.dto.AuthDtos.LoginRequest;
import com.faultlens.api.dto.AuthDtos.TokenResponse;
import com.faultlens.api.exception.GlobalExceptionHandler;
import com.faultlens.api.security.LoginLockedException;
import com.faultlens.api.service.AuthService;
import com.faultlens.api.service.AuthService.AuthSession;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

class AuthControllerTest {
    private MockMvc mockMvc;
    private AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, true, Duration.ofDays(7)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static AuthSession session(String access, String refresh) {
        return new AuthSession(new TokenResponse(access, Instant.now().plusSeconds(900)), refresh);
    }

    @Test
    void loginReturnsTheAccessTokenAndSetsAnHttpOnlyRefreshCookie() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(session("access-token", "refresh-1"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("access-token"))
                .andExpect(jsonPath("$.data.expires_at").exists())
                .andExpect(jsonPath("$.data.refresh_token").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                        containsString("faultlens_refresh=refresh-1"),
                        containsString("HttpOnly"),
                        containsString("Secure"),
                        containsString("SameSite=Strict"),
                        containsString("Path=/api/v1/auth"),
                        containsString("Max-Age=604800"))));
    }

    @Test
    void refreshReadsTheCookieAndRotatesIt() throws Exception {
        when(authService.refresh("refresh-1")).thenReturn(session("new-access-token", "refresh-2"));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("faultlens_refresh", "refresh-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("new-access-token"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("faultlens_refresh=refresh-2")));
    }

    @Test
    void refreshWithoutAValidCookieIs401() throws Exception {
        when(authService.refresh(null)).thenThrow(new BadCredentialsException("Session expired"));

        mockMvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesAndClearsTheCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").cookie(new Cookie("faultlens_refresh", "refresh-1")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        verify(authService).logout("refresh-1");
    }

    @Test
    void lockedLoginIs429WithRetryAfter() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new LoginLockedException(Duration.ofMinutes(15)));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "x"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "900"))
                .andExpect(jsonPath("$.error.code").value("LOGIN_LOCKED"));
    }
}
