CREATE TABLE log_groups (
    id            BIGSERIAL PRIMARY KEY,
    fingerprint   VARCHAR(64)  NOT NULL UNIQUE,
    first_message TEXT,
    severity      VARCHAR(20)  NOT NULL,
    count         BIGINT       DEFAULT 1,
    source_id     BIGINT       REFERENCES log_sources(id) ON DELETE SET NULL,
    first_seen_at TIMESTAMPTZ DEFAULT NOW(),
    last_seen_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_log_groups_severity  ON log_groups(severity);
CREATE INDEX idx_log_groups_last_seen ON log_groups(last_seen_at DESC);
CREATE INDEX idx_log_groups_source    ON log_groups(source_id);
