package com.faultlens.api.controller;

import com.faultlens.api.dto.AlarmRuleDtos.AlarmRuleRequest;
import com.faultlens.api.dto.AlarmRuleDtos.AlarmRuleResponse;
import com.faultlens.api.service.AlarmRuleService;
import com.faultlens.common.dto.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
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

/**
 * Alarm rule CRUD (ADR 0001).
 */
@RestController
@RequestMapping("/api/v1/alarms/rules")
@RequiredArgsConstructor
public class AlarmRuleController {
    private final AlarmRuleService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AlarmRuleResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(service.list()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AlarmRuleResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.get(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AlarmRuleResponse>> create(@Valid @RequestBody AlarmRuleRequest request,
                                                                 Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.create(request, principal == null ? null : principal.getName())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AlarmRuleResponse>> update(@PathVariable Long id,
                                                                 @Valid @RequestBody AlarmRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, request)));
    }

    @PostMapping("/{id}/enable")
    public ResponseEntity<ApiResponse<AlarmRuleResponse>> enable(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.setEnabled(id, true)));
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<AlarmRuleResponse>> disable(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.setEnabled(id, false)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
