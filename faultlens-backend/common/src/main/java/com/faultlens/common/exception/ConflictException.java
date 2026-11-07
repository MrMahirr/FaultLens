package com.faultlens.common.exception;

/**
 * The request conflicts with the current state (e.g. a unique value already taken); mapped to 409.
 */
public class ConflictException extends FaultLensException {
    public ConflictException(String message, String errorCode) {
        super(message, errorCode);
    }
}
