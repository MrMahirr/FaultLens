package com.faultlens.api.controller;

import com.faultlens.api.dto.ApiKeyDtos.ApiKeyResponse;
import com.faultlens.api.dto.ApiKeyDtos.CreateApiKeyRequest;
import com.faultlens.api.dto.ApiKeyDtos.CreatedApiKeyResponse;
import com.faultlens.api.security.Roles;
import com.faultlens.api.service.ApiKeyService;
import com.faultlens.common.dto.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API key management, ADMIN only (an API key itself can never reach these endpoints: no scope
 * covers them).
 */
@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
@PreAuthorize(Roles.ADMIN_ONLY)
public class ApiKeyController {
    private final ApiKeyService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(service.list()));
    }

    /** Returns the full token once; it cannot be retrieved later. */
    @PostMapping
    public ResponseEntity<ApiResponse<CreatedApiKeyResponse>> create(@Valid @RequestBody CreateApiKeyRequest request,
                                                                      Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.create(request, principal.getName())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable Long id, Principal principal) {
        service.revoke(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
