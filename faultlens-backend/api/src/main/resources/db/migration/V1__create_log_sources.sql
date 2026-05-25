CREATE TABLE log_sources (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    type         VARCHAR(50)  NOT NULL,
    config       JSONB,
    enabled      BOOLEAN      DEFAULT true,
    created_at   TIMESTAMPTZ  DEFAULT NOW(),
    last_seen_at TIMESTAMPTZ
);
