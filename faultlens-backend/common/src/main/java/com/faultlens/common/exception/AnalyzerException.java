package com.faultlens.common.exception;

public class AnalyzerException extends FaultLensException {
    /**
     * Creates an analyzer exception.
     */
    public AnalyzerException(String message, String errorCode) {
        super(message, errorCode);
    }

    /**
     * Creates an analyzer exception with a cause.
     */
    public AnalyzerException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
