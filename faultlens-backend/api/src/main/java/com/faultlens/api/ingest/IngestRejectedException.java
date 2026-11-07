package com.faultlens.api.ingest;

import com.faultlens.common.exception.FaultLensException;
import org.springframework.http.HttpStatus;

/**
 * A push that is refused as a whole, with its HTTP status (400 / 403 / 409 / 413 / 503). Nothing of
 * the request was written.
 */
public class IngestRejectedException extends FaultLensException {
    private final HttpStatus status;
    /** Seconds for Retry-After (503 only), or null. */
    private final Long retryAfterSeconds;

    public IngestRejectedException(HttpStatus status, String code, String message) {
        this(status, code, message, null, null);
    }

    public IngestRejectedException(HttpStatus status, String code, String message, Long retryAfterSeconds, Throwable cause) {
        super(message, code, cause);
        this.status = status;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
