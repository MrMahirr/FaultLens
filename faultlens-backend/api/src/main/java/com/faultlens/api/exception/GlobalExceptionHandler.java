package com.faultlens.api.exception;

import com.faultlens.api.dto.ErrorEnvelope;
import com.faultlens.common.exception.FaultLensException;
import com.faultlens.common.exception.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * Handles missing domain resources.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorEnvelope.of(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * Handles conflicts with existing state (409).
     */
    @ExceptionHandler(com.faultlens.common.exception.ConflictException.class)
    public ResponseEntity<ErrorEnvelope> handleConflict(com.faultlens.common.exception.ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorEnvelope.of(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * Handles a username locked after too many failed logins (429 + Retry-After).
     */
    @ExceptionHandler(com.faultlens.api.security.LoginLockedException.class)
    public ResponseEntity<ErrorEnvelope> handleLoginLocked(com.faultlens.api.security.LoginLockedException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", Long.toString(ex.getRetryAfter().toSeconds()))
                .body(ErrorEnvelope.of(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * Handles domain exceptions.
     */
    @ExceptionHandler(FaultLensException.class)
    public ResponseEntity<ErrorEnvelope> handlefaultlens(FaultLensException ex) {
        return ResponseEntity.badRequest().body(ErrorEnvelope.of(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * Handles missing entities.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorEnvelope.of("RESOURCE_NOT_FOUND", ex.getMessage()));
    }

    /**
     * Handles validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorEnvelope> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.unprocessableEntity().body(ErrorEnvelope.of("VALIDATION_FAILED", message));
    }

    /**
     * Handles constraint violations.
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ErrorEnvelope> handleConstraintViolation(jakarta.validation.ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ErrorEnvelope.of("VALIDATION_FAILED", message));
    }

    /**
     * Handles invalid credentials.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorEnvelope> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorEnvelope.of("AUTH_FAILED", ex.getMessage()));
    }

    /**
     * Handles access denied errors.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorEnvelope> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorEnvelope.of("ACCESS_DENIED", ex.getMessage()));
    }

    /**
     * Handles malformed request parameters / bodies (e.g. {@code ?status=NOPE}, invalid JSON).
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorEnvelope> handleBadRequest(Exception ex) {
        String message = ex instanceof MethodArgumentTypeMismatchException mismatch
                ? "Invalid value for parameter '" + mismatch.getName() + "'"
                : "Malformed request body";
        return ResponseEntity.badRequest().body(ErrorEnvelope.of("BAD_REQUEST", message));
    }

    /**
     * Handles unexpected errors.
     *
     * <p>Spring MVC's own exceptions (unknown URL, wrong HTTP method, missing parameter, ...) carry
     * their status and keep it instead of becoming 500. Everything else is logged and answered with
     * a generic message, so internal details do not reach the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorEnvelope> handleUnexpected(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            String code = status.value() == HttpStatus.NOT_FOUND.value() ? "NOT_FOUND" : "REQUEST_REJECTED";
            return ResponseEntity.status(status).body(ErrorEnvelope.of(code, errorResponse.getBody().getDetail()));
        }
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorEnvelope.of("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
