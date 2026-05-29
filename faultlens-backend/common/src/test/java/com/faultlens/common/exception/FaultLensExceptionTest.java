package com.faultlens.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FaultLensExceptionTest {
    @Test
    void storesMessageAndErrorCodeUsingPublicConstructorOrder() {
        FaultLensException exception = new FaultLensException("Something failed", "SAMPLE_ERROR");

        assertThat(exception).hasMessage("Something failed");
        assertThat(exception.getErrorCode()).isEqualTo("SAMPLE_ERROR");
    }

    @Test
    void resourceNotFoundUsesStableDefaultCode() {
        ResourceNotFoundException exception = new ResourceNotFoundException("LogEntry with id 42 not found");

        assertThat(exception).hasMessage("LogEntry with id 42 not found");
        assertThat(exception.getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
    }
}
