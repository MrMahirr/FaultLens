CREATE TABLE log_entries (
    id             BIGSERIAL PRIMARY KEY,
    source_id      BIGINT       REFERENCES log_sources(id) ON DELETE SET NULL,
    group_id       BIGINT       REFERENCES log_groups(id) ON DELETE SET NULL,
    severity       VARCHAR(20)  NOT NULL,
    message        TEXT         NOT NULL,
    parsed_message TEXT,
    stack_trace    TEXT,
    raw_line       TEXT,
    namespace      VARCHAR(255),
    pod_name       VARCHAR(255),
    container_name VARCHAR(255),
    service_name   VARCHAR(255),
    cluster        VARCHAR(255),
    timestamp      TIMESTAMPTZ  NOT NULL,
    created_at     TIMESTAMPTZ  DEFAULT NOW()
);
CREATE INDEX idx_log_entries_timestamp ON log_entries(timestamp DESC);
CREATE INDEX idx_log_entries_severity  ON log_entries(severity);
CREATE INDEX idx_log_entries_group     ON log_entries(group_id);
CREATE INDEX idx_log_entries_source_ts ON log_entries(source_id, timestamp DESC);
