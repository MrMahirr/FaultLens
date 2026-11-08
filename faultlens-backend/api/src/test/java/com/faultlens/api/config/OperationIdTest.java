package com.faultlens.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.faultlens.api.controller.ApiKeyController;
import com.faultlens.api.controller.LogController;
import com.faultlens.api.controller.SourceController;
import com.faultlens.api.ingest.IngestController;
import org.junit.jupiter.api.Test;

/** Operation ids are derived from controller + method name, never from discovery order. */
class OperationIdTest {
    @Test
    void prefixesTheMethodWithTheController() {
        assertThat(OpenApiConfig.operationId(SourceController.class, "list")).isEqualTo("sourceList");
        assertThat(OpenApiConfig.operationId(LogController.class, "groupEntries")).isEqualTo("logGroupEntries");
        assertThat(OpenApiConfig.operationId(ApiKeyController.class, "revoke")).isEqualTo("apiKeyRevoke");
    }

    @Test
    void doesNotRepeatAPrefixTheMethodAlreadyHas() {
        assertThat(OpenApiConfig.operationId(IngestController.class, "ingestJson")).isEqualTo("ingestJson");
    }
}
