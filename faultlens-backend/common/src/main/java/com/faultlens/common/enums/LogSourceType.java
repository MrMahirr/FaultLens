package com.faultlens.common.enums;

public enum LogSourceType {
    KUBERNETES,
    SSH,
    DOCKER,
    CICD,
    LOCAL_FILE,
    /**
     * Pushed by the application itself (HTTP / OTLP ingest, P10 / 1.2), through an API key bound
     * to the source. Nothing is collected: there is no adapter and no lease.
     */
    HTTP;

    /** True for sources that send their logs to FaultLens instead of being read by the collector. */
    public boolean isPushed() {
        return this == HTTP;
    }
}
