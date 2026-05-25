package com.loglens.common.exception;

public class ProcessorException extends LogLensException {
    /**
     * Creates a processor exception.
     */
    public ProcessorException(String message, String errorCode) {
        super(message, errorCode);
    }

    /**
     * Creates a processor exception with a cause.
     */
    public ProcessorException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
