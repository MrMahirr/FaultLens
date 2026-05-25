package com.loglens.common.exception;

public class CollectorException extends LogLensException {
    /**
     * Creates a collector exception.
     */
    public CollectorException(String message, String errorCode) {
        super(message, errorCode);
    }

    /**
     * Creates a collector exception with a cause.
     */
    public CollectorException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
