package com.faultlens.api.controller;

import com.faultlens.api.dto.LogGroupDtos.GroupStatusUpdateRequest;
import com.faultlens.api.dto.LogGroupDtos.LogGroupEventDto;
import com.faultlens.api.service.LogGroupLifecycleService;
import com.faultlens.api.service.LogQueryService;
import com.faultlens.common.enums.GroupStatus;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.faultlens.common.dto.ApiResponse;
import com.faultlens.common.enums.Severity;
import java.time.Instant;
import java.util.Map;
import com.faultlens.api.security.Roles;
import com.faultlens.common.dto.PagedResponse;
import com.faultlens.common.dto.LogEntryDto;
import com.faultlens.api.dto.LogDetailDto;
import com.faultlens.common.dto.LogGroupDto;
import com.faultlens.api.dto.ClearedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
@Validated
public class LogController {
    private final LogQueryService service;
    private final LogGroupLifecycleService lifecycle;

    /**
     * Lists logs with pagination and filters.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<LogEntryDto>>> list(
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(1000) int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.list(severity, sourceId, from, to, search, page, size)));
    }

    /**
     * Returns one log detail including analyses.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LogDetailDto>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.detail(id)));
    }

    /**
     * Lists log groups.
     */
    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<PagedResponse<LogGroupDto>>> groups(
            @RequestParam(required = false) Severity severity,
            @Parameter(description = "Statuses to list (repeat or comma-separate); default UNRESOLVED,RESOLVED")
            @RequestParam(required = false) List<GroupStatus> status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(1000) int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.groups(severity, status, page, size)));
    }

    /**
     * Returns one log group.
     */
    @GetMapping("/groups/{id}")
    public ResponseEntity<ApiResponse<LogGroupDto>> group(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(lifecycle.get(id)));
    }

    /**
     * Resolves, ignores or reopens a log group (P10 / 2.1). 409 when it already has that status or
     * changed meanwhile (e.g. regressed).
     */
    @PreAuthorize(Roles.CAN_EDIT)
    @PatchMapping("/groups/{id}")
    public ResponseEntity<ApiResponse<LogGroupDto>> updateGroupStatus(@PathVariable Long id,
                                                                      @Valid @RequestBody GroupStatusUpdateRequest request,
                                                                      Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(lifecycle.updateStatus(id, request,
                principal == null ? null : principal.getName())));
    }

    /**
     * The group's timeline, newest first (at most 200 entries).
     */
    @GetMapping("/groups/{id}/events")
    public ResponseEntity<ApiResponse<List<LogGroupEventDto>>> groupEvents(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(lifecycle.timeline(id)));
    }

    /**
     * Lists log entries for a group.
     */
    @GetMapping("/groups/{id}/entries")
    public ResponseEntity<ApiResponse<PagedResponse<LogEntryDto>>> groupEntries(
            @PathVariable Long id, 
            @RequestParam(defaultValue = "0") @Min(0) int page, 
            @RequestParam(defaultValue = "50") @Min(1) @Max(1000) int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.groupEntries(id, page, size)));
    }

    /**
     * Returns last-hour severity stats.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> stats(@RequestParam(defaultValue = "60") int windowMinutes) {
        return ResponseEntity.ok(ApiResponse.ok(service.stats(windowMinutes)));
    }

    /**
     * Clears logs for a specific source.
     */
    @PreAuthorize(Roles.ADMIN_ONLY)
    @org.springframework.web.bind.annotation.DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<ClearedResponse>> clearLogs(@RequestParam Long sourceId) {
        service.clearLogsBySource(sourceId);
        return ResponseEntity.ok(ApiResponse.ok(new ClearedResponse(true)));
    }
}
