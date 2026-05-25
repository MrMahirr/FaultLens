package com.loglens.api.controller;

import com.loglens.api.dto.DeploymentDto;
import com.loglens.api.service.DeploymentService;
import com.loglens.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/deployments")
@RequiredArgsConstructor
public class DeploymentController {
    private final DeploymentService service;

    /**
     * Lists deployment history.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.list(page, size)));
    }

    /**
     * Creates a deployment record.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody DeploymentDto deployment) {
        return ResponseEntity.ok(ApiResponse.ok(service.create(deployment)));
    }
}
