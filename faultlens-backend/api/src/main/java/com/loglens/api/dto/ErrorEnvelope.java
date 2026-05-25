package com.loglens.api.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorEnvelope {
    private boolean success;
    private ErrorBody error;

    @Data
    @Builder
    public static class ErrorBody {
        private String code;
        private String message;
        private Instant timestamp;
    }

    /**
     * Creates a standard error response body.
     */
    public static ErrorEnvelope of(String code, String message) {
        return ErrorEnvelope.builder()
                .success(false)
                .error(ErrorBody.builder().code(code).message(message).timestamp(Instant.now()).build())
                .build();
    }
}
