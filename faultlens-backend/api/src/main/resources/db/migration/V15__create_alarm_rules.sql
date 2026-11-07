-- Alarm rules (ADR 0001). Evaluated in the api module on every processed log entry.
CREATE TABLE alarm_rules (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(200) NOT NULL,
    type          VARCHAR(20)  NOT NULL CHECK (type IN ('THRESHOLD', 'PATTERN', 'NEW_GROUP')),
    -- minimum severity an entry/group needs to count for the rule
    severity      VARCHAR(20)  NOT NULL CHECK (severity IN ('TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'CRITICAL')),
    -- THRESHOLD: entries in the last window_sec >= threshold
    window_sec    INTEGER CHECK (window_sec BETWEEN 60 AND 3600),
    threshold     INTEGER CHECK (threshold >= 1),
    -- PATTERN: Java regular expression matched against the message
    pattern       VARCHAR(500),
    -- empty = all sources
    source_ids    BIGINT[]     NOT NULL DEFAULT '{}',
    channels      VARCHAR(20)[] NOT NULL DEFAULT '{IN_APP}',
    cooldown_sec  INTEGER      NOT NULL DEFAULT 900 CHECK (cooldown_sec BETWEEN 0 AND 86400),
    enabled       BOOLEAN      NOT NULL DEFAULT true,
    created_by    VARCHAR(100),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT alarm_rules_threshold_fields CHECK (type <> 'THRESHOLD' OR (window_sec IS NOT NULL AND threshold IS NOT NULL)),
    CONSTRAINT alarm_rules_pattern_fields CHECK (type <> 'PATTERN' OR pattern IS NOT NULL)
);

CREATE INDEX idx_alarm_rules_enabled ON alarm_rules (enabled);

-- Alarms raised by a rule point at it; deleting the rule keeps the alarm history.
-- alarms.rule_id (text) stays for built-in alarm sources such as AI_ANALYSIS.
ALTER TABLE alarms ADD COLUMN alarm_rule_id BIGINT REFERENCES alarm_rules (id) ON DELETE SET NULL;
CREATE INDEX idx_alarms_alarm_rule_id ON alarms (alarm_rule_id);
