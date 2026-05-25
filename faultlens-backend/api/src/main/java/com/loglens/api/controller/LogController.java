package com.loglens.api.controller;

import com.loglens.api.service.LogQueryService;
import com.loglens.common.dto.ApiResponse;
import com.loglens.common.enums.Severity;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogController {
    private final LogQueryService service;

    /**
     * Lists logs with pagination and filters.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> list(
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.list(severity, sourceId, from, to, search, page, size)));
    }

    /**
     * Returns one log detail including analyses.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.detail(id)));
    }

    /**
     * Lists log groups.
     */
    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<?>> groups(
            @RequestParam(required = false) Severity severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.groups(severity, page, size)));
    }

    /**
     * Lists log entries for a group.
     */
    @GetMapping("/groups/{id}/entries")
    public ResponseEntity<ApiResponse<?>> groupEntries(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.groupEntries(id, page, size)));
    }

    /**
     * Returns last-hour severity stats.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<?>> stats(@RequestParam(defaultValue = "60") int windowMinutes) {
        return ResponseEntity.ok(ApiResponse.ok(service.stats(windowMinutes)));
    }
}
