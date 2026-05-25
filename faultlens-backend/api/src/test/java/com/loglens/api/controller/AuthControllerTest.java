package com.loglens.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.loglens.api.dto.AuthDtos.LoginRequest;
import com.loglens.api.dto.AuthDtos.RefreshRequest;
import com.loglens.api.dto.AuthDtos.TokenResponse;
import com.loglens.api.exception.GlobalExceptionHandler;
import com.loglens.api.service.AuthService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {
    private MockMvc mockMvc;
    private AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void loginReturnsTokenResponseOnSuccess() throws Exception {
        LoginRequest request = new LoginRequest("admin", "password");
        TokenResponse tokenResponse = new TokenResponse("access-token", Instant.now().plusSeconds(3600));
        
        when(authService.login(any(LoginRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("access-token"))
                .andExpect(jsonPath("$.data.expires_at").exists());

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void refreshReturnsNewTokensOnSuccess() throws Exception {
        RefreshRequest request = new RefreshRequest("refresh-token");
        TokenResponse tokenResponse = new TokenResponse("new-access-token", Instant.now().plusSeconds(3600));
        
        when(authService.refresh(any(RefreshRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("new-access-token"));

        verify(authService).refresh(any(RefreshRequest.class));
    }
}
