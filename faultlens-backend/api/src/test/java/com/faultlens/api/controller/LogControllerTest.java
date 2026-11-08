package com.faultlens.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.faultlens.api.dto.LogDetailDto;
import com.faultlens.api.exception.GlobalExceptionHandler;
import com.faultlens.api.dto.LogGroupDtos.GroupStatusUpdateRequest;
import com.faultlens.api.service.LogGroupCollaborationService;
import com.faultlens.api.service.LogGroupLifecycleService;
import com.faultlens.api.service.LogQueryService;
import com.faultlens.common.dto.LogGroupDto;
import com.faultlens.common.enums.GroupStatus;
import com.faultlens.common.exception.ConflictException;
import org.springframework.http.MediaType;
import com.faultlens.common.dto.LogEntryDto;
import com.faultlens.common.dto.PagedResponse;
import com.faultlens.common.enums.Severity;
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
    private LogGroupLifecycleService lifecycle;

    @BeforeEach
    void setUp() {
        service = mock(LogQueryService.class);
        lifecycle = mock(LogGroupLifecycleService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new LogController(service, lifecycle, mock(LogGroupCollaborationService.class)))
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

    @Test
    void groupsPassTheStatusFilter() throws Exception {
        when(service.groups(any(), any(), any(), anyInt(), anyInt())).thenReturn(new PagedResponse<>(List.of(), 0, 50, 0L, 0));

        mockMvc.perform(get("/api/v1/logs/groups").param("status", "UNRESOLVED,IGNORED"))
                .andExpect(status().isOk());

        verify(service).groups(null, List.of(GroupStatus.UNRESOLVED, GroupStatus.IGNORED), null, 0, 50);
    }

    @Test
    void patchGroupChangesItsStatus() throws Exception {
        Instant until = Instant.parse("2030-01-01T00:00:00Z");
        when(lifecycle.updateStatus(eq(5L), any(), any()))
                .thenReturn(LogGroupDto.builder().id(5L).status(GroupStatus.IGNORED).ignoreUntil(until).build());

        mockMvc.perform(patch("/api/v1/logs/groups/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"IGNORED\", \"ignore_until\": \"2030-01-01T00:00:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IGNORED"));

        verify(lifecycle).updateStatus(eq(5L), eq(new GroupStatusUpdateRequest(GroupStatus.IGNORED, until)), any());
    }

    @Test
    void patchGroupWithoutStatusIsRejected() throws Exception {
        mockMvc.perform(patch("/api/v1/logs/groups/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                // Bean validation status as the app maps it (422 here); the point is it never reaches the service.
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(lifecycle);
    }

    @Test
    void patchGroupConflictIs409() throws Exception {
        when(lifecycle.updateStatus(eq(5L), any(), any()))
                .thenThrow(new ConflictException("Log group is already RESOLVED", "GROUP_STATUS_UNCHANGED"));

        mockMvc.perform(patch("/api/v1/logs/groups/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"RESOLVED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("GROUP_STATUS_UNCHANGED"));
    }
}
