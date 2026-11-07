package com.faultlens.api.controller;

import com.faultlens.api.dto.UserDtos.CreateUserRequest;
import com.faultlens.api.dto.UserDtos.ResetPasswordRequest;
import com.faultlens.api.dto.UserDtos.UpdateRoleRequest;
import com.faultlens.api.dto.UserDtos.UserResponse;
import com.faultlens.api.security.Roles;
import com.faultlens.api.service.UserService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User management; every endpoint is ADMIN only.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize(Roles.ADMIN_ONLY)
public class UserController {
    private final UserService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(service.list()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.create(request)));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> changeRole(@PathVariable Long id,
                                                                @Valid @RequestBody UpdateRoleRequest request,
                                                                Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.changeRole(id, request.role(), principal.getName())));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<UserResponse>> resetPassword(@PathVariable Long id,
                                                                   @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.resetPassword(id, request.password())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        service.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
