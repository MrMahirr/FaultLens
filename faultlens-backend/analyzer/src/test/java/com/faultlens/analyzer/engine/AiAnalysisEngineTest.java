package com.faultlens.analyzer.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AiAnalysisEngineTest {
    @Test
    void unavailableWithoutApiKey() {
        AiAnalysisEngine engine = new AiAnalysisEngine("", "gpt-4o-mini", new ObjectMapper());

        assertThat(engine.isAvailable()).isFalse();
    }

    @Test
    void availableWithApiKey() {
        AiAnalysisEngine engine = new AiAnalysisEngine("test-key", "gpt-4o-mini", new ObjectMapper());

        assertThat(engine.isAvailable()).isTrue();
    }
}
