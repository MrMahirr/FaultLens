package com.loglens.common.exception;

public class ResourceNotFoundException extends LogLensException {
    /**
     * Creates a not-found exception for an application resource.
     */
    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND");
    }

    /**
     * Creates a not-found exception with an explicit error code.
     */
    public ResourceNotFoundException(String message, String errorCode) {
        super(message, errorCode);
    }
}
