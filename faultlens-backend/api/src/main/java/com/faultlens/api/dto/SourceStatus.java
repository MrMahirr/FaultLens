package com.faultlens.api.dto;

/**
 * Connection state shown on the Sources page (values match the frontend's ConnectionStatus).
 */
public enum SourceStatus {
    /** Enabled and its adapter is streaming. */
    CONNECTED,
    /** Enabled, but the adapter is not running (refused its config, failed or gave up). */
    WARNING,
    /** Disabled by the user. */
    DISCONNECTED;

    /**
     * Derives the state from the stored flag and the collector's live state.
     */
    public static SourceStatus of(boolean enabled, boolean streaming) {
        if (!enabled) {
            return DISCONNECTED;
        }
        return streaming ? CONNECTED : WARNING;
    }
}
