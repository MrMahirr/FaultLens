package com.faultlens.api.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.faultlens.common.enums.Severity;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class JsonLogDecoderTest {
    private final JsonLogDecoder decoder = new JsonLogDecoder(new ObjectMapper());

    private List<PushedLog> decode(String json) {
        return decoder.decode(json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void decodesAnArrayOfRecords() {
        List<PushedLog> logs = decode("""
                [{"timestamp":"2026-11-07T10:00:00Z","severity":"warning","message":"slow","service_name":"web",
                  "pod_name":"web-1","attributes":{"route":"/checkout","ms":812}},
                 {"timestamp":1762509600000,"level":"err","msg":"boom","stackTrace":"x\\n\\tat a.B.c(B.java:1)"}]""");

        assertThat(logs).hasSize(2);
        assertThat(logs.get(0).timestamp()).isEqualTo(Instant.parse("2026-11-07T10:00:00Z"));
        assertThat(logs.get(0).severity()).isEqualTo(Severity.WARN);
        assertThat(logs.get(0).serviceName()).isEqualTo("web");
        assertThat(logs.get(0).podName()).isEqualTo("web-1");
        assertThat(logs.get(0).attributes()).containsEntry("route", "/checkout").containsEntry("ms", "812");
        assertThat(logs.get(1).timestamp()).isEqualTo(Instant.ofEpochMilli(1762509600000L));
        assertThat(logs.get(1).severity()).isEqualTo(Severity.ERROR);
        assertThat(logs.get(1).message()).isEqualTo("boom");
        assertThat(logs.get(1).stackTrace()).startsWith("x\n");
    }

    @Test
    void acceptsAWrapperObjectAndASingleRecord() {
        assertThat(decode("{\"logs\":[{\"message\":\"a\"},{\"message\":\"b\"}]}")).hasSize(2);
        assertThat(decode("{\"message\":\"only one\"}")).singleElement()
                .satisfies(log -> assertThat(log.severity()).isNull()); // left to the processor
    }

    @Test
    void rejectsInvalidInput() {
        assertThatThrownBy(() -> decode("not json")).isInstanceOf(IngestRejectedException.class);
        assertThatThrownBy(() -> decode("[1, 2]")).hasMessageContaining("Record 0");
        assertThatThrownBy(() -> decode("[{\"message\":\"x\",\"timestamp\":\"yesterday\"}]"))
                .hasMessageContaining("timestamp");
        assertThatThrownBy(() -> decode("\"text\"")).isInstanceOf(IngestRejectedException.class);
    }
}
