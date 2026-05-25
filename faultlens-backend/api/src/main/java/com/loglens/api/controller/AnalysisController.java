package com.loglens.api.controller;

import com.loglens.api.service.AnalysisQueryService;
import com.loglens.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analyses")
@RequiredArgsConstructor
public class AnalysisController {
    private final AnalysisQueryService service;

    /**
     * Lists analyses.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.list(page, size)));
    }

    /**
     * Returns one analysis.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.get(id)));
    }

    /**
     * Triggers manual analysis for a group.
     */
    @PostMapping("/trigger/{groupId}")
    public ResponseEntity<ApiResponse<?>> trigger(@PathVariable Long groupId) {
        return ResponseEntity.ok(ApiResponse.ok(service.trigger(groupId)));
    }
}
