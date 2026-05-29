package com.faultlens.api.controller;

import com.faultlens.api.mapper.ApiDtoMapper;
import com.faultlens.collector.dto.LogSourceCreateRequest;
import com.faultlens.collector.dto.LogSourceUpdateRequest;
import com.faultlens.collector.service.LogSourceService;
import com.faultlens.common.dto.ApiResponse;
import java.util.Map;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    /**
     * Lists sources.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> list() {
        return ResponseEntity.ok(ApiResponse.ok(service.findAll().stream()
                .map(mapper::toLogSourceDto)
                .toList()));
    }

    /**
     * Creates a source.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody LogSourceCreateRequest source) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(mapper.toLogSourceDto(service.create(source))));
    }

    /**
     * Updates a source.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> update(@PathVariable Long id, @Valid @RequestBody LogSourceUpdateRequest source) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toLogSourceDto(service.update(id, source))));
    }

    /**
     * Deletes a source.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Tests source connectivity.
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<ApiResponse<?>> test(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("connected", service.testConnection(id))));
    }

    /**
     * Enables a source.
     */
    @PostMapping("/{id}/enable")
    public ResponseEntity<ApiResponse<?>> enable(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toLogSourceDto(service.enable(id))));
    }

    /**
     * Disables a source.
     */
    @PostMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<?>> disable(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toLogSourceDto(service.disable(id))));
    }
}
