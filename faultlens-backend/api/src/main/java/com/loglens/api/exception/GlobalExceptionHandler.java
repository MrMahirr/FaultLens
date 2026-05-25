package com.loglens.api.exception;

import com.loglens.api.dto.ErrorEnvelope;
import com.loglens.common.exception.LogLensException;
import com.loglens.common.exception.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
     * Handles domain exceptions.
     */
    @ExceptionHandler(LogLensException.class)
    public ResponseEntity<ErrorEnvelope> handleLogLens(LogLensException ex) {
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
     * Handles unexpected errors.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorEnvelope> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorEnvelope.of("INTERNAL_ERROR", ex.getMessage()));
    }
}
