-- P10 / 1.1: scoped service tokens (API keys) for CI and integrations, instead of a user JWT.
-- Token format: flk_<prefix>_<secret>. Only the SHA-256 of the whole token is stored; the prefix
-- is kept in clear to find the row and to show the key in the UI. A key is shown once.
CREATE TABLE api_keys (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    prefix       VARCHAR(16)  NOT NULL UNIQUE,
    secret_hash  VARCHAR(64)  NOT NULL,
    -- comma-separated ApiKeyScope values, e.g. "deployments:write,logs:read"
    scopes       VARCHAR(500) NOT NULL,
    created_by   VARCHAR(100),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at   TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    last_used_ip VARCHAR(45),
    revoked_at   TIMESTAMPTZ
);

CREATE INDEX idx_api_keys_active ON api_keys (prefix) WHERE revoked_at IS NULL;
