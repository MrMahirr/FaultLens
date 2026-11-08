package com.faultlens.api.controller;

import com.faultlens.api.dto.LogGroupDtos.AssignRequest;
import com.faultlens.api.dto.LogGroupDtos.AssignableUser;
import com.faultlens.api.dto.LogGroupDtos.CommentRequest;
import com.faultlens.api.dto.LogGroupDtos.GroupStatusUpdateRequest;
import com.faultlens.api.dto.LogGroupDtos.LogGroupCommentDto;
import com.faultlens.api.dto.LogGroupDtos.MergeRequest;
import com.faultlens.api.dto.LogContextDto;
import com.faultlens.api.service.LogContextService;
import com.faultlens.api.service.LogGroupCollaborationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    private final LogGroupCollaborationService collaboration;
    private final LogContextService contextService;

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
     * The entries of the same source and pod around this one (P10 / 2.3), oldest first, the entry
     * itself included; within ±60 minutes. Load more by asking for the first / last entry's
     * context with {@code after=0} / {@code before=0}.
     */
    @GetMapping("/{id}/context")
    public ResponseEntity<ApiResponse<LogContextDto>> context(
            @PathVariable Long id,
            @RequestParam(defaultValue = "50") @Min(0) @Max(LogContextService.MAX_LINES) int before,
            @RequestParam(defaultValue = "50") @Min(0) @Max(LogContextService.MAX_LINES) int after) {
        return ResponseEntity.ok(ApiResponse.ok(contextService.context(id, before, after)));
    }

    /**
     * Lists log groups.
     */
    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<PagedResponse<LogGroupDto>>> groups(
            @RequestParam(required = false) Severity severity,
            @Parameter(description = "Statuses to list (repeat or comma-separate); default UNRESOLVED,RESOLVED")
            @RequestParam(required = false) List<GroupStatus> status,
            @Parameter(description = "Only groups assigned to this username")
            @RequestParam(required = false) String assignee,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(1000) int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.groups(severity, status, assignee, page, size)));
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
     * Assigns the group to a user, or unassigns it with a null/blank assignee (P10 / 2.2).
     */
    @PreAuthorize(Roles.CAN_EDIT)
    @PutMapping("/groups/{id}/assignee")
    public ResponseEntity<ApiResponse<LogGroupDto>> assignGroup(@PathVariable Long id,
                                                                @Valid @RequestBody AssignRequest request,
                                                                Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(collaboration.assign(id, request.assignee(), name(principal))));
    }

    /**
     * Users a group can be assigned to (names only; editors may not list users).
     */
    @PreAuthorize(Roles.CAN_EDIT)
    @GetMapping("/groups/assignees")
    public ResponseEntity<ApiResponse<List<AssignableUser>>> groupAssignees() {
        return ResponseEntity.ok(ApiResponse.ok(collaboration.assignableUsers()));
    }

    /**
     * The group's comments, oldest first.
     */
    @GetMapping("/groups/{id}/comments")
    public ResponseEntity<ApiResponse<List<LogGroupCommentDto>>> groupComments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(collaboration.comments(id)));
    }

    @PreAuthorize(Roles.CAN_EDIT)
    @PostMapping("/groups/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<LogGroupCommentDto>> addGroupComment(@PathVariable Long id,
                                                                          @Valid @RequestBody CommentRequest request,
                                                                          Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(collaboration.addComment(id, request.body(), name(principal))));
    }

    /** Only the author edits (403 otherwise). */
    @PreAuthorize(Roles.CAN_EDIT)
    @PutMapping("/groups/{id}/comments/{commentId}")
    public ResponseEntity<ApiResponse<LogGroupCommentDto>> editGroupComment(@PathVariable Long id,
                                                                           @PathVariable Long commentId,
                                                                           @Valid @RequestBody CommentRequest request,
                                                                           Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                collaboration.editComment(id, commentId, request.body(), name(principal))));
    }

    /** The author or an admin deletes (403 otherwise). */
    @PreAuthorize(Roles.CAN_EDIT)
    @DeleteMapping("/groups/{id}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteGroupComment(@PathVariable Long id, @PathVariable Long commentId,
                                                   Authentication authentication) {
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> ("ROLE_" + Roles.ADMIN).equals(authority.getAuthority()));
        collaboration.deleteComment(id, commentId, authentication == null ? null : authentication.getName(), admin);
        return ResponseEntity.noContent().build();
    }

    /**
     * Merges this group into {@code target_id}: its new entries go there. 409 if already merged.
     */
    @PreAuthorize(Roles.CAN_EDIT)
    @PostMapping("/groups/{id}/merge")
    public ResponseEntity<ApiResponse<LogGroupDto>> mergeGroup(@PathVariable Long id,
                                                               @Valid @RequestBody MergeRequest request,
                                                               Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(collaboration.merge(id, request.targetId(), name(principal))));
    }

    /**
     * Undoes a merge (admins only). 409 if the group is not merged.
     */
    @PreAuthorize(Roles.ADMIN_ONLY)
    @PostMapping("/groups/{id}/unmerge")
    public ResponseEntity<ApiResponse<LogGroupDto>> unmergeGroup(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(collaboration.unmerge(id, name(principal))));
    }

    private static String name(Principal principal) {
        return principal == null ? null : principal.getName();
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
