package com.faultlens.api.controller;

import com.faultlens.api.dto.DeploymentDto;
import com.faultlens.api.service.DeploymentService;
import com.faultlens.common.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import com.faultlens.api.security.Roles;
import com.faultlens.common.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/api/v1/deployments")
@RequiredArgsConstructor
public class DeploymentController {
    private final DeploymentService service;

    /**
     * Lists deployment history, newest first. {@code q}: part of the service name
     * (case-insensitive); {@code environment}: exact environment.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<DeploymentDto>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String environment,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.list(q, environment, page, size)));
    }

    /**
     * Creates a deployment record.
     */
    @PreAuthorize(Roles.CAN_RECORD_DEPLOYMENT)
    @PostMapping
    public ResponseEntity<ApiResponse<DeploymentDto>> create(@Valid @RequestBody DeploymentDto deployment) {
        return ResponseEntity.ok(ApiResponse.ok(service.create(deployment)));
    }
}
