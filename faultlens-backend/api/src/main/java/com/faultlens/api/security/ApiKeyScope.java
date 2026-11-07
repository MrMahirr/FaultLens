package com.faultlens.api.security;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * What an API key may call. A key is limited to the endpoints of its scopes: any other request
 * (including ones open to every signed-in user, and the auth endpoints) is rejected, so a leaked
 * key exposes only what it was created for.
 */
public enum ApiKeyScope {
    /** CI records deployments. */
    DEPLOYMENTS_WRITE("deployments:write", HttpMethod.POST, "/api/v1/deployments"),
    /** Applications push logs (HTTP / OTLP ingest, P10 / 1.2). */
    LOGS_INGEST("logs:ingest", HttpMethod.POST, "/api/v1/ingest/**", "/v1/logs"),
    LOGS_READ("logs:read", HttpMethod.GET, "/api/v1/logs", "/api/v1/logs/**"),
    ALARMS_READ("alarms:read", HttpMethod.GET, "/api/v1/alarms", "/api/v1/alarms/**");

    private final String value;
    private final HttpMethod method;
    private final List<PathPattern> paths;

    ApiKeyScope(String value, HttpMethod method, String... paths) {
        this.value = value;
        this.method = method;
        this.paths = Arrays.stream(paths).map(PathPatternParser.defaultInstance::parse).toList();
    }

    /** Wire / storage name, e.g. {@code deployments:write}. */
    public String value() {
        return value;
    }

    /** Spring authority granted to a key with this scope. */
    public String authority() {
        return "SCOPE_" + value;
    }

    public boolean allows(String httpMethod, String path) {
        if (!method.matches(httpMethod)) {
            return false;
        }
        PathContainer container = PathContainer.parsePath(path);
        return paths.stream().anyMatch(p -> p.matches(container));
    }

    public static Optional<ApiKeyScope> fromValue(String value) {
        return Arrays.stream(values()).filter(s -> s.value.equals(value)).findFirst();
    }
}
