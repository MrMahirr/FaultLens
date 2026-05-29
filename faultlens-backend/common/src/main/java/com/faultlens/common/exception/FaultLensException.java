package com.faultlens.common.exception;

import lombok.Getter;

@Getter
public class FaultLensException extends RuntimeException {
    private final String errorCode;

    /**
     * Creates an application exception with a stable error code.
     */
    public FaultLensException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Creates an application exception with a stable error code and cause.
     */
    public FaultLensException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
