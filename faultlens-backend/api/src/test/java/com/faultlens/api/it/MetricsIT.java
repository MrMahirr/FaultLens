package com.faultlens.api.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.faultlens.collector.kafka.LogEventProducer;
import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.Severity;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * P08: Prometheus metrics. Own Spring context: actuator on a separate management port, where the
 * scrape needs no credentials; on the main port it stays ADMIN-only.
 */
class MetricsIT extends AbstractIntegrationTest {
    private static final int MANAGEMENT_PORT = freePort();

    @DynamicPropertySource
    static void managementPort(DynamicPropertyRegistry registry) {
        registry.add("management.server.port", () -> MANAGEMENT_PORT);
    }

    @Autowired
    private LogEventProducer producer;

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static HttpResponse<String> scrape(int port) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/prometheus")).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void theScrapeIsOpenOnlyOnTheManagementPort() throws Exception {
        assertThat(scrape(MANAGEMENT_PORT).statusCode()).isEqualTo(200);
        assertThat(scrape(port).statusCode()).as("main port").isEqualTo(401);
        // health probes stay on the main port for container health checks
        assertThat(HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/readyz")).build(),
                HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
        // and on the management port nothing else is opened
        assertThat(HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + MANAGEMENT_PORT + "/actuator/metrics")).build(),
                HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
    }

    @Test
    void ingestAlarmAndKafkaMetricsAreExported() throws Exception {
        String marker = "metrics-" + UUID.randomUUID();
        producer.send(LogEventDto.builder()
                .severity(Severity.INFO)
                .message("metrics probe " + marker)
                .timestamp(Instant.now())
                .build());

        await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            String body = scrape(MANAGEMENT_PORT).body();
            assertThat(body).contains("faultlens_logs_ingested_total{")
                    .containsPattern("faultlens_logs_ingested_total\\{[^}]*source=\"none\"");
            // consumer lag per group, from the Micrometer Kafka client metrics
            assertThat(body).contains("kafka_consumer_fetch_manager_records_lag_max");
            // computed lag per group/topic (end offset - committed offset)
            assertThat(body).containsPattern("faultlens_kafka_consumer_lag\\{[^}]*group=\"faultlens-processor\"[^}]*topic=\"log-raw\"");
            assertThat(body).contains("application=\"faultlens\"");
        });
    }
}
