package com.faultlens.api.controller;

import com.faultlens.api.mapper.ApiDtoMapper;
import com.faultlens.api.dto.LogSourceDto;
import com.faultlens.api.dto.SourceStatus;
import com.faultlens.collector.model.LogSource;
import com.faultlens.collector.service.CollectorOrchestrator;
import com.faultlens.collector.dto.LogSourceCreateRequest;
import com.faultlens.collector.dto.LogSourceUpdateRequest;
import com.faultlens.collector.service.LogSourceService;
import com.faultlens.common.dto.ApiResponse;
import java.util.Map;
import jakarta.validation.Valid;
import com.faultlens.api.security.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sources")
@RequiredArgsConstructor
public class SourceController {
    private final LogSourceService service;
    private final ApiDtoMapper mapper;
    private final CollectorOrchestrator orchestrator;

    /**
     * Lists sources.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> list() {
        return ResponseEntity.ok(ApiResponse.ok(service.findAll().stream()
                .map(this::toDto)
                .toList()));
    }

    /**
     * Creates a source.
     */
    @PreAuthorize(Roles.CAN_EDIT)
    @PostMapping
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody LogSourceCreateRequest source) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(toDto(service.create(source))));
    }

    /**
     * Updates a source.
     */
    @PreAuthorize(Roles.CAN_EDIT)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> update(@PathVariable Long id, @Valid @RequestBody LogSourceUpdateRequest source) {
        return ResponseEntity.ok(ApiResponse.ok(toDto(service.update(id, source))));
    }

    /**
     * Deletes a source.
     */
    @PreAuthorize(Roles.ADMIN_ONLY)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Tests source connectivity for an existing source.
     */
    @PreAuthorize(Roles.CAN_EDIT)
    @PostMapping("/{id}/test")
    public ResponseEntity<ApiResponse<?>> test(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("connected", service.testConnection(id))));
    }

    /**
     * Tests source connectivity before saving.
     */
    @PreAuthorize(Roles.CAN_EDIT)
    @PostMapping("/test-config")
    public ResponseEntity<ApiResponse<?>> testConfig(@Valid @RequestBody LogSourceCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("connected", service.testConnection(request))));
    }

    /**
     * Enables a source.
     */
    @PreAuthorize(Roles.CAN_EDIT)
    @PostMapping("/{id}/enable")
    public ResponseEntity<ApiResponse<?>> enable(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(toDto(service.enable(id))));
    }

    /**
     * Disables a source.
     */
    @PreAuthorize(Roles.CAN_EDIT)
    @PostMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<?>> disable(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(toDto(service.disable(id))));
    }

    private LogSourceDto toDto(LogSource source) {
        LogSourceDto dto = mapper.toLogSourceDto(source);
        // Any instance may be collecting it (P08 leases); a failing adapter is not "connected".
        boolean collecting = orchestrator.isCollectedAnywhere(source.getId()) && !"ERROR".equals(source.getStatus());
        dto.setStatus(SourceStatus.of(source.isEnabled(), collecting));
        return dto;
    }
}
