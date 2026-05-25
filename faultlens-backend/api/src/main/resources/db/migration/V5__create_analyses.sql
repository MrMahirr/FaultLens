CREATE TABLE analyses (
    id                  BIGSERIAL PRIMARY KEY,
    log_group_id        BIGINT       REFERENCES log_groups(id)  ON DELETE CASCADE,
    log_entry_id        BIGINT       REFERENCES log_entries(id) ON DELETE CASCADE,
    root_cause          TEXT,
    suggestion          TEXT,
    affected_deployment VARCHAR(255),
    confidence_score    DOUBLE PRECISION,
    engine_type         VARCHAR(50),
    analyzed_at         TIMESTAMPTZ  DEFAULT NOW()
);
CREATE INDEX idx_analyses_group ON analyses(log_group_id);
CREATE INDEX idx_analyses_entry ON analyses(log_entry_id);
