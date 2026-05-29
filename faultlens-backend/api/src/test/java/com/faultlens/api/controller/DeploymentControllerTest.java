package com.faultlens.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.faultlens.api.dto.DeploymentDto;
import com.faultlens.api.exception.GlobalExceptionHandler;
import com.faultlens.api.service.DeploymentService;
import com.faultlens.common.dto.PagedResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DeploymentControllerTest {
    private MockMvc mockMvc;
    private DeploymentService service;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        service = mock(DeploymentService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new DeploymentController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listReturnsPagedDeployments() throws Exception {
        DeploymentDto dto = new DeploymentDto();
        dto.setId(1L);
        dto.setServiceName("auth-service");
        dto.setVersion("v1.0.0");
        dto.setEnvironment("production");
        dto.setDeployedAt(Instant.now());

        PagedResponse<DeploymentDto> response = new PagedResponse<>(List.of(dto), 0, 1, 1L, 1);
        when(service.list(anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/v1/deployments")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.content[0].version").value("v1.0.0"))
                .andExpect(jsonPath("$.data.content[0].environment").value("production"));

        verify(service).list(0, 10);
    }

    @Test
    void createReturnsCreatedDeployment() throws Exception {
        DeploymentDto dto = new DeploymentDto();
        dto.setServiceName("auth-service");
        dto.setVersion("v1.1.0");
        dto.setEnvironment("staging");
        dto.setDeployedAt(Instant.now());

        DeploymentDto saved = new DeploymentDto();
        saved.setId(2L);
        saved.setServiceName("auth-service");
        saved.setVersion("v1.1.0");
        saved.setEnvironment("staging");
        saved.setDeployedAt(dto.getDeployedAt());

        when(service.create(any(DeploymentDto.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2L))
                .andExpect(jsonPath("$.data.version").value("v1.1.0"));

        verify(service).create(any(DeploymentDto.class));
    }
}
