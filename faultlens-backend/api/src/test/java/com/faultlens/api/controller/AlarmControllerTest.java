package com.faultlens.api.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.faultlens.api.dto.AlarmDto;
import com.faultlens.api.dto.AlarmStatsDto;
import com.faultlens.api.exception.GlobalExceptionHandler;
import com.faultlens.api.model.AlarmStatus;
import com.faultlens.api.service.AlarmService;
import com.faultlens.common.dto.PagedResponse;
import com.faultlens.common.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AlarmControllerTest {
    private MockMvc mockMvc;
    private AlarmService service;

    @BeforeEach
    void setUp() {
        service = mock(AlarmService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AlarmController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listIsVersionedWrappedPagedAndSnakeCase() throws Exception {
        AlarmDto alarm = AlarmDto.builder()
                .id(1L).ruleName("rule").severity("CRITICAL").status(AlarmStatus.ACTIVE)
                .sourceId(3L).triggeredAt(Instant.parse("2026-11-03T10:00:00Z")).build();
        when(service.list(eq(AlarmStatus.ACTIVE), eq("CRITICAL"), anyInt(), anyInt()))
                .thenReturn(new PagedResponse<>(List.of(alarm), 0, 20, 1L, 1));

        mockMvc.perform(get("/api/v1/alarms").param("status", "ACTIVE").param("severity", "CRITICAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].rule_name").value("rule"))
                .andExpect(jsonPath("$.data.content[0].source_id").value(3))
                .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.total_elements").value(1));

        verify(service).list(AlarmStatus.ACTIVE, "CRITICAL", 0, 20);
    }

    @Test
    void statsEndpoint() throws Exception {
        when(service.stats()).thenReturn(new AlarmStatsDto(4, 2, 9, 3));

        mockMvc.perform(get("/api/v1/alarms/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(4))
                .andExpect(jsonPath("$.data.open_critical").value(3));
    }

    @Test
    void ackAndResolveReturnUpdatedAlarm() throws Exception {
        when(service.acknowledge(1L)).thenReturn(AlarmDto.builder().id(1L).status(AlarmStatus.ACKNOWLEDGED).build());
        when(service.resolveAlarm(1L)).thenReturn(AlarmDto.builder().id(1L).status(AlarmStatus.RESOLVED).build());

        mockMvc.perform(put("/api/v1/alarms/1/ack"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACKNOWLEDGED"));
        mockMvc.perform(put("/api/v1/alarms/1/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));
    }

    @Test
    void unknownAlarmIs404NotServerError() throws Exception {
        when(service.resolveAlarm(99L)).thenThrow(new ResourceNotFoundException("Alarm not found: 99"));
        doThrow(new ResourceNotFoundException("Alarm not found: 99")).when(service).deleteAlarm(99L);

        mockMvc.perform(put("/api/v1/alarms/99/resolve")).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/alarms/99")).andExpect(status().isNotFound());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/alarms/1")).andExpect(status().isNoContent());
        verify(service).deleteAlarm(1L);
    }
}
