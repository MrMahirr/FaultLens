package com.faultlens.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faultlens.api.exception.GlobalExceptionHandler;
import com.faultlens.api.mapper.ApiDtoMapper;
import com.faultlens.collector.dto.LogSourceCreateRequest;
import com.faultlens.collector.dto.LogSourceUpdateRequest;
import com.faultlens.collector.model.LogSource;
import com.faultlens.collector.service.LogSourceService;
import com.faultlens.common.enums.LogSourceType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SourceControllerTest {
    private MockMvc mockMvc;
    private LogSourceService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = mock(LogSourceService.class);
        ApiDtoMapper mapper = new ApiDtoMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new SourceController(service, mapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listReturnsAllLogSources() throws Exception {
        LogSource source = new LogSource();
        source.setId(1L);
        source.setName("App Server");
        source.setType(LogSourceType.SSH);
        source.setEnabled(true);

        when(service.findAll()).thenReturn(List.of(source));

        mockMvc.perform(get("/api/v1/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].name").value("App Server"))
                .andExpect(jsonPath("$.data[0].type").value("SSH"));

        verify(service).findAll();
    }

    @Test
    void createReturns201CreatedAndSourceDto() throws Exception {
        LogSourceCreateRequest request = new LogSourceCreateRequest();
        request.setName("New Source");
        request.setType(LogSourceType.SSH);
        request.setConfig("{\"host\": \"localhost\"}");

        LogSource saved = new LogSource();
        saved.setId(2L);
        saved.setName("New Source");
        saved.setType(LogSourceType.SSH);
        saved.setEnabled(true);

        when(service.create(any(LogSourceCreateRequest.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2L))
                .andExpect(jsonPath("$.data.name").value("New Source"));

        verify(service).create(any(LogSourceCreateRequest.class));
    }

    @Test
    void updateReturnsUpdatedSource() throws Exception {
        LogSourceUpdateRequest request = new LogSourceUpdateRequest();
        request.setName("Updated Source");
        request.setType(LogSourceType.SSH);

        LogSource updated = new LogSource();
        updated.setId(1L);
        updated.setName("Updated Source");
        updated.setType(LogSourceType.SSH);

        when(service.update(eq(1L), any(LogSourceUpdateRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/sources/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Source"));

        verify(service).update(eq(1L), any(LogSourceUpdateRequest.class));
    }

    @Test
    void deleteReturns204NoContent() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete("/api/v1/sources/1"))
                .andExpect(status().isNoContent());

        verify(service).delete(1L);
    }

    @Test
    void testConnectionReturnsConnectedState() throws Exception {
        when(service.testConnection(1L)).thenReturn(true);

        mockMvc.perform(post("/api/v1/sources/1/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.connected").value(true));

        verify(service).testConnection(1L);
    }

    @Test
    void enableReturnsEnabledSource() throws Exception {
        LogSource source = new LogSource();
        source.setId(1L);
        source.setEnabled(true);

        when(service.enable(1L)).thenReturn(source);

        mockMvc.perform(post("/api/v1/sources/1/enable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));

        verify(service).enable(1L);
    }

    @Test
    void disableReturnsDisabledSource() throws Exception {
        LogSource source = new LogSource();
        source.setId(1L);
        source.setEnabled(false);

        when(service.disable(1L)).thenReturn(source);

        mockMvc.perform(post("/api/v1/sources/1/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        verify(service).disable(1L);
    }
}
