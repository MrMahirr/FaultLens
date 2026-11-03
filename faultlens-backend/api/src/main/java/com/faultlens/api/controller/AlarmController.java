package com.faultlens.api.controller;

import com.faultlens.api.dto.AlarmDto;
import com.faultlens.api.dto.AlarmStatsDto;
import com.faultlens.api.model.AlarmStatus;
import com.faultlens.api.service.AlarmService;
import com.faultlens.common.dto.ApiResponse;
import com.faultlens.common.dto.PagedResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/alarms")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    /**
     * Lists alarms (newest first) with optional status / severity filters.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AlarmDto>>> list(
            @RequestParam(required = false) AlarmStatus status,
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return ResponseEntity.ok(ApiResponse.ok(alarmService.list(status, severity, page, size)));
    }

    /**
     * Returns alarm counters per state.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AlarmStatsDto>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(alarmService.stats()));
    }

    /**
     * Acknowledges an alarm.
     */
    @PutMapping("/{id}/ack")
    public ResponseEntity<ApiResponse<AlarmDto>> acknowledge(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(alarmService.acknowledge(id)));
    }

    /**
     * Resolves an alarm.
     */
    @PutMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<AlarmDto>> resolve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(alarmService.resolveAlarm(id)));
    }

    /**
     * Deletes an alarm.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        alarmService.deleteAlarm(id);
        return ResponseEntity.noContent().build();
    }
}
