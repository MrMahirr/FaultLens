CREATE TABLE system_settings (
    id                  BIGSERIAL PRIMARY KEY,
    log_retention_days  INTEGER DEFAULT 30,
    default_engine      VARCHAR(50) DEFAULT 'ai',
    language            VARCHAR(10) DEFAULT 'tr',
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

-- Insert a singleton record for system-wide settings
INSERT INTO system_settings (log_retention_days, default_engine, language)
VALUES (30, 'ai', 'tr');
