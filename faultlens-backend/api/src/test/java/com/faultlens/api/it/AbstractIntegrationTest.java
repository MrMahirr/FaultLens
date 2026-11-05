package com.faultlens.api.it;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for *IT tests: the whole application (collector, processor, analyzer, api) on a
 * random port, against real PostgreSQL, Kafka (KRaft) and Redis containers — the same images as
 * docker-compose.yml.
 *
 * <p>Containers are started once per JVM and shared by every IT class (the Spring context is
 * cached too), so tests must not assume an empty database: scope assertions to the data a test
 * created itself.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
    /** Matches application-test.yml; the first login must replace it. */
    protected static final String ADMIN_INITIAL_PASSWORD = "it-initial-pass-1";
    /** Password set by {@link #adminToken()} after the forced first-login change. */
    protected static final String ADMIN_PASSWORD = "it-admin-pass-2";

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:4.4.0"));
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8.10-alpine").withExposedPorts(6379);

    private static String adminToken;

    static {
        POSTGRES.start();
        KAFKA.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate rest;

    /**
     * Returns an unrestricted admin token. On first use it performs the real first-login flow:
     * login with the initial password (restricted token) → change password → fresh token.
     */
    protected String adminToken() {
        if (adminToken != null) {
            return adminToken;
        }
        ResponseEntity<JsonNode> login = login("admin", ADMIN_PASSWORD);
        if (login.getStatusCode() != HttpStatus.OK) {
            login = login("admin", ADMIN_INITIAL_PASSWORD);
            assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(login.getBody().at("/data/must_change_password").asBoolean()).isTrue();
            String restricted = login.getBody().at("/data/token").asText();

            login = exchange(HttpMethod.PUT, "/api/v1/auth/password", restricted,
                    Map.of("current_password", ADMIN_INITIAL_PASSWORD, "new_password", ADMIN_PASSWORD));
            assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
        assertThat(login.getBody().at("/data/must_change_password").asBoolean()).isFalse();
        adminToken = login.getBody().at("/data/token").asText();
        return adminToken;
    }

    protected ResponseEntity<JsonNode> login(String username, String password) {
        return exchange(HttpMethod.POST, "/api/v1/auth/login", null,
                Map.of("username", username, "password", password));
    }

    protected ResponseEntity<JsonNode> get(String path, String token) {
        return exchange(HttpMethod.GET, path, token, null);
    }

    protected ResponseEntity<JsonNode> exchange(HttpMethod method, String path, String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange(path, method, new HttpEntity<>(body, headers), JsonNode.class);
    }
}
