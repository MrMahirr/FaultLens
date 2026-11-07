package com.faultlens.api.controller;

import com.faultlens.api.service.AnalysisQueryService;
import com.faultlens.common.dto.ApiResponse;
import com.faultlens.api.security.Roles;
import com.faultlens.common.dto.PagedResponse;
import com.faultlens.common.dto.AnalysisResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public ResponseEntity<ApiResponse<PagedResponse<AnalysisResultDto>>> list(
            @RequestParam(required = false) Long sourceId,
            @RequestParam(defaultValue = "0") int page, 
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.list(sourceId, page, size)));
    }

    /**
     * Clears history.
     */
    @PreAuthorize(Roles.ADMIN_ONLY)
    @org.springframework.web.bind.annotation.DeleteMapping
    public ResponseEntity<ApiResponse<String>> deleteHistory(@RequestParam(required = false) Long sourceId) {
        service.deleteHistory(sourceId);
        return ResponseEntity.ok(ApiResponse.ok("Analiz geçmişi başarıyla temizlendi."));
    }

    /**
     * Returns one analysis.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AnalysisResultDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.get(id)));
    }

    /**
     * Triggers manual analysis for a group.
     */
    @PreAuthorize(Roles.CAN_EDIT)
    @PostMapping("/trigger/{groupId}")
    public ResponseEntity<ApiResponse<AnalysisResultDto>> trigger(@PathVariable Long groupId) {
        return ResponseEntity.ok(ApiResponse.ok(service.trigger(groupId)));
    }
}
