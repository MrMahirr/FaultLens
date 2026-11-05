package com.faultlens.analyzer.engine;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AiAnalysisEngineTest {
    @Test
    void unavailableWithoutApiKey() {
        AiAnalysisEngine engine = new AiAnalysisEngine(true, "", "gpt-4o-mini", new ObjectMapper());

        assertThat(engine.isAvailable()).isFalse();
    }

    @Test
    void availableWithApiKey() {
        AiAnalysisEngine engine = new AiAnalysisEngine(true, "test-key", "gpt-4o-mini", new ObjectMapper());

        assertThat(engine.isAvailable()).isTrue();
    }

    @Test
    void unavailableWhenSwitchedOffEvenWithApiKey() {
        // AI_ENABLED=false is the deployment-level kill switch (cost, data leaving the system).
        AiAnalysisEngine engine = new AiAnalysisEngine(false, "test-key", "gpt-4o-mini", new ObjectMapper());

        assertThat(engine.isAvailable()).isFalse();
    }
}
