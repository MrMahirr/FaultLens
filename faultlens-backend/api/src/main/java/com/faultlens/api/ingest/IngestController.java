package com.faultlens.api.ingest;

import com.faultlens.api.security.ApiKeyAuthFilter;
import com.faultlens.common.dto.ApiResponse;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Log ingest for applications (P10 / 1.2). Only API keys with the {@code logs:ingest} scope may
 * call it (not user tokens); the key decides the source.
 *
 * <ul>
 *   <li>{@code POST /api/v1/ingest/logs} — simple JSON (see {@link JsonLogDecoder}), answers
 *       {@code 202 {"accepted": n}}.</li>
 *   <li>{@code POST /v1/logs} — OTLP/HTTP, protobuf ({@code application/x-protobuf}) or JSON,
 *       answers {@code 200} with an {@code ExportLogsServiceResponse} in the same encoding; the
 *       OpenTelemetry Collector's {@code otlphttp} exporter can point straight at the API.</li>
 * </ul>
 */
@RestController
@PreAuthorize("hasAuthority('SCOPE_logs:ingest')")
public class IngestController {
    static final MediaType PROTOBUF = MediaType.parseMediaType("application/x-protobuf");

    private final IngestService service;
    private final JsonLogDecoder jsonDecoder;
    private final OtlpLogDecoder otlpDecoder;
    private final int maxBodyBytes;

    public IngestController(IngestService service, JsonLogDecoder jsonDecoder, OtlpLogDecoder otlpDecoder,
                            @Value("${faultlens.ingest.max-body-bytes:5242880}") int maxBodyBytes) {
        this.service = service;
        this.jsonDecoder = jsonDecoder;
        this.otlpDecoder = otlpDecoder;
        this.maxBodyBytes = maxBodyBytes;
    }

    @PostMapping(path = "/api/v1/ingest/logs", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Integer>>> ingestJson(HttpServletRequest request) throws IOException {
        List<PushedLog> records = jsonDecoder.decode(readBody(request));
        int accepted = service.ingest(sourceOf(request), records, "json");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(Map.of("accepted", accepted)));
    }

    // One OTLP operation with two encodings: both handlers share the id (OpenApiConfig).
    @Operation(operationId = "ingestOtlpLogs")
    @PostMapping(path = "/v1/logs", consumes = "application/x-protobuf", produces = "application/x-protobuf")
    public ResponseEntity<byte[]> ingestOtlpProtobuf(HttpServletRequest request) throws IOException {
        service.ingest(sourceOf(request), otlpDecoder.decodeProtobuf(readBody(request)), "otlp");
        return ResponseEntity.ok().contentType(PROTOBUF)
                .body(ExportLogsServiceResponse.getDefaultInstance().toByteArray());
    }

    // One OTLP operation with two encodings: both handlers share the id (OpenApiConfig).
    @Operation(operationId = "ingestOtlpLogs")
    @PostMapping(path = "/v1/logs", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> ingestOtlpJson(HttpServletRequest request) throws IOException {
        service.ingest(sourceOf(request), otlpDecoder.decodeJson(readBody(request)), "otlp");
        return ResponseEntity.ok(Map.of()); // ExportLogsServiceResponse with no partial_success
    }

    private static Long sourceOf(HttpServletRequest request) {
        return request.getAttribute(ApiKeyAuthFilter.SOURCE_ATTRIBUTE) instanceof Long id ? id : null;
    }

    /** Reads at most {@code maxBodyBytes}; larger bodies (declared or streamed) are 413. */
    private byte[] readBody(HttpServletRequest request) throws IOException {
        long declared = request.getContentLengthLong();
        if (declared > maxBodyBytes) {
            throw tooLarge();
        }
        try (InputStream in = request.getInputStream()) {
            byte[] body = in.readNBytes(maxBodyBytes + 1);
            if (body.length > maxBodyBytes) {
                throw tooLarge();
            }
            return body;
        }
    }

    private IngestRejectedException tooLarge() {
        return new IngestRejectedException(HttpStatus.CONTENT_TOO_LARGE, "PAYLOAD_TOO_LARGE",
                "Request body is larger than " + maxBodyBytes + " bytes");
    }
}
