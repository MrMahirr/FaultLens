-- P10 / 2.1: log group lifecycle (UNRESOLVED / RESOLVED / IGNORED) and regression detection.
-- See docs/adr/0004-log-group-lifecycle.md.
--
-- Rollback (manual, no data worth keeping is lost):
--   DROP TABLE log_group_events;
--   DROP INDEX idx_log_groups_status_last_seen;
--   ALTER TABLE log_groups DROP COLUMN status, DROP COLUMN resolved_at, DROP COLUMN resolved_by,
--     DROP COLUMN ignore_until, DROP COLUMN regressed_at, DROP COLUMN regression_count;
--   DELETE FROM alarm_rules WHERE type = 'REGRESSION';  -- then restore the V15 type check.

-- Adding columns with constant defaults is a metadata-only change in PostgreSQL (no rewrite).
ALTER TABLE log_groups
    ADD COLUMN status           VARCHAR(16)  NOT NULL DEFAULT 'UNRESOLVED'
        CHECK (status IN ('UNRESOLVED', 'RESOLVED', 'IGNORED')),
    ADD COLUMN resolved_at      TIMESTAMPTZ,
    -- Username, like alarm_rules.created_by: survives the user being deleted.
    ADD COLUMN resolved_by      VARCHAR(100),
    -- IGNORED until this time; NULL = until manually reopened.
    ADD COLUMN ignore_until     TIMESTAMPTZ,
    ADD COLUMN regressed_at     TIMESTAMPTZ,
    ADD COLUMN regression_count INT          NOT NULL DEFAULT 0;

-- Groups view: "unresolved, most recent first".
CREATE INDEX idx_log_groups_status_last_seen ON log_groups (status, last_seen_at DESC);

-- One timeline per group: lifecycle changes now, assignment/comments/merges in 2.2.
CREATE TABLE log_group_events (
    id         BIGSERIAL    PRIMARY KEY,
    group_id   BIGINT       NOT NULL REFERENCES log_groups (id) ON DELETE CASCADE,
    -- CREATED, RESOLVED, IGNORED, REOPENED, REGRESSED, UNIGNORED (2.2: ASSIGNED, COMMENTED, MERGED)
    type       VARCHAR(32)  NOT NULL,
    -- Username; NULL for transitions made by the system (regression, ignore expiry).
    actor      VARCHAR(100),
    payload    JSONB,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_log_group_events_group ON log_group_events (group_id, created_at DESC);

-- Groups that existed before this migration get no CREATED event (their timeline starts empty).

ALTER TABLE alarm_rules DROP CONSTRAINT alarm_rules_type_check;
ALTER TABLE alarm_rules ADD CONSTRAINT alarm_rules_type_check
    CHECK (type IN ('THRESHOLD', 'PATTERN', 'NEW_GROUP', 'REGRESSION'));
