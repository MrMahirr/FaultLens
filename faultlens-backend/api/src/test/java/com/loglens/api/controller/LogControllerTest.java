package com.loglens.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.loglens.api.dto.LogDetailDto;
import com.loglens.api.exception.GlobalExceptionHandler;
import com.loglens.api.service.LogQueryService;
import com.loglens.common.dto.LogEntryDto;
import com.loglens.common.dto.PagedResponse;
import com.loglens.common.enums.Severity;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LogControllerTest {
    private MockMvc mockMvc;
    private LogQueryService service;

    @BeforeEach
    void setUp() {
        service = mock(LogQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new LogController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listReturnsPagedLogs() throws Exception {
        LogEntryDto entry = LogEntryDto.builder()
                .id(1L)
                .message("Log message")
                .severity(Severity.ERROR)
                .build();

        PagedResponse<LogEntryDto> response = new PagedResponse<>(List.of(entry), 0, 1, 1L, 1);
        when(service.list(any(), any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/v1/logs")
                        .param("severity", "ERROR")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.content[0].severity").value("ERROR"))
                .andExpect(jsonPath("$.data.content[0].message").value("Log message"));

        verify(service).list(eq(Severity.ERROR), any(), any(), any(), any(), eq(0), eq(10));
    }

    @Test
    void detailReturnsLogDetailDto() throws Exception {
        LogDetailDto detail = new LogDetailDto();
        detail.setLog(LogEntryDto.builder()
                .id(1L)
                .message("Detailed log message")
                .build());
        detail.setAnalyses(List.of());

        when(service.detail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/logs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.log.id").value(1L))
                .andExpect(jsonPath("$.data.log.message").value("Detailed log message"));

        verify(service).detail(1L);
    }

    @Test
    void statsReturnsWindowStats() throws Exception {
        Map<String, Long> stats = Map.of("ERROR", 10L, "WARN", 5L, "total", 15L);
        when(service.stats(60)).thenReturn(stats);

        mockMvc.perform(get("/api/v1/logs/stats").param("windowMinutes", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ERROR").value(10L))
                .andExpect(jsonPath("$.data.total").value(15L));

        verify(service).stats(60);
    }
}
