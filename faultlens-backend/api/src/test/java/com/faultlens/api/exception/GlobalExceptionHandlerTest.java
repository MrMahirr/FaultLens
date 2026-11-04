package com.faultlens.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.faultlens.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsResourceNotFoundTo404() {
        var response = handler.handleResourceNotFound(new ResourceNotFoundException("Log not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void mapsAccessDeniedTo403() {
        var response = handler.handleAccessDenied(new AccessDeniedException("Denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("ACCESS_DENIED");
    }

    @Test
    void unknownUrlIs404NotServerError() {
        var response = handler.handleUnexpected(new NoResourceFoundException(HttpMethod.GET, "/api/alarms", "api/alarms"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getError().getCode()).isEqualTo("NOT_FOUND");
    }

    @Test
    void wrongHttpMethodKeepsItsStatus() {
        var response = handler.handleUnexpected(new HttpRequestMethodNotSupportedException("PATCH"));

        assertThat(response.getStatusCode().value()).isEqualTo(405);
    }

    @Test
    void unexpectedErrorDoesNotLeakInternalMessage() {
        var response = handler.handleUnexpected(new IllegalStateException("jdbc:postgresql://db:5432 password=x"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getError().getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getError().getMessage()).doesNotContain("jdbc", "password");
    }
}
