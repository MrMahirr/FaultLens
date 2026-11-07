package com.faultlens.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiKeyScopeTest {

    @Test
    void deploymentScopeCoversOnlyCreatingDeployments() {
        ApiKeyScope scope = ApiKeyScope.DEPLOYMENTS_WRITE;

        assertThat(scope.allows("POST", "/api/v1/deployments")).isTrue();
        assertThat(scope.allows("GET", "/api/v1/deployments")).isFalse();
        assertThat(scope.allows("POST", "/api/v1/deployments/1")).isFalse();
        assertThat(scope.allows("POST", "/api/v1/sources")).isFalse();
    }

    @Test
    void readScopesCoverListAndDetailButNotWrites() {
        assertThat(ApiKeyScope.LOGS_READ.allows("GET", "/api/v1/logs")).isTrue();
        assertThat(ApiKeyScope.LOGS_READ.allows("GET", "/api/v1/logs/42")).isTrue();
        assertThat(ApiKeyScope.LOGS_READ.allows("DELETE", "/api/v1/logs/source/1")).isFalse();
        assertThat(ApiKeyScope.ALARMS_READ.allows("GET", "/api/v1/alarms/stats")).isTrue();
        assertThat(ApiKeyScope.ALARMS_READ.allows("POST", "/api/v1/alarms/1/resolve")).isFalse();
    }

    @Test
    void noScopeReachesAuthUsersOrKeyManagement() {
        for (ApiKeyScope scope : ApiKeyScope.values()) {
            for (String method : new String[] {"GET", "POST", "PUT", "DELETE"}) {
                assertThat(scope.allows(method, "/api/v1/auth/login")).isFalse();
                assertThat(scope.allows(method, "/api/v1/users")).isFalse();
                assertThat(scope.allows(method, "/api/v1/api-keys")).isFalse();
                assertThat(scope.allows(method, "/api/v1/settings/system")).isFalse();
            }
        }
    }

    @Test
    void parsesWireValues() {
        assertThat(ApiKeyScope.fromValue("logs:ingest")).contains(ApiKeyScope.LOGS_INGEST);
        assertThat(ApiKeyScope.fromValue("admin")).isEmpty();
        assertThat(ApiKeyScope.DEPLOYMENTS_WRITE.authority()).isEqualTo("SCOPE_deployments:write");
    }
}
