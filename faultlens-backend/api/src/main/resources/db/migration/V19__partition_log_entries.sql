-- P08: log_entries becomes RANGE-partitioned by day (UTC) so retention drops whole days
-- instead of deleting millions of rows, and time-bounded queries touch only their days.
--
-- Layout: log_entries_YYYYMMDD partitions [day, day + 1) plus log_entries_default for rows
-- outside them (very old timestamps, clock skew). LogPartitionMaintenance creates the coming
-- days every night; LogRetentionJob detaches + drops expired days and batch-deletes old rows
-- from the default partition.
--
-- Existing rows are copied into the new table inside this migration's transaction.

-- analyses.log_entry_id stays as informational column; a foreign key to a partitioned table
-- would have to include the partition key. Analyses have their own retention (analyzed_at).
ALTER TABLE analyses DROP CONSTRAINT IF EXISTS analyses_log_entry_id_fkey;

-- Creates the daily partitions from_day..to_day (inclusive) that do not exist yet. A day whose
-- rows already sit in the default partition is skipped (creating it would fail); those rows
-- are handled by the default-partition retention.
CREATE OR REPLACE FUNCTION faultlens_ensure_log_partitions(from_day date, to_day date)
RETURNS integer LANGUAGE plpgsql AS $$
DECLARE
    d date := from_day;
    created integer := 0;
    part text;
    lower_bound timestamptz;
    upper_bound timestamptz;
BEGIN
    WHILE d <= to_day LOOP
        part := 'log_entries_' || to_char(d, 'YYYYMMDD');
        lower_bound := d::timestamp AT TIME ZONE 'UTC';
        upper_bound := (d + 1)::timestamp AT TIME ZONE 'UTC';
        IF to_regclass(part) IS NULL THEN
            IF EXISTS (SELECT 1 FROM log_entries_default WHERE timestamp >= lower_bound AND timestamp < upper_bound) THEN
                RAISE NOTICE 'skipping %: rows for that day are in log_entries_default', part;
            ELSE
                EXECUTE format('CREATE TABLE %I PARTITION OF log_entries FOR VALUES FROM (%L) TO (%L)',
                               part, lower_bound, upper_bound);
                created := created + 1;
            END IF;
        END IF;
        d := d + 1;
    END LOOP;
    RETURN created;
END;
$$;

ALTER TABLE log_entries RENAME TO log_entries_unpartitioned;
ALTER INDEX log_entries_pkey RENAME TO log_entries_unpartitioned_pkey;

CREATE TABLE log_entries (
    id             BIGINT       NOT NULL DEFAULT nextval('log_entries_id_seq'),
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
    created_at     TIMESTAMPTZ  DEFAULT NOW(),
    PRIMARY KEY (id, timestamp)
) PARTITION BY RANGE (timestamp);

CREATE TABLE log_entries_default PARTITION OF log_entries DEFAULT;

-- Days that have data, within the retention window (no retention: the last 400 days), through
-- one week ahead. Older rows land in the default partition.
SELECT faultlens_ensure_log_partitions(
    GREATEST(COALESCE((SELECT min(timestamp AT TIME ZONE 'UTC')::date FROM log_entries_unpartitioned), current_date),
             current_date - COALESCE(NULLIF((SELECT max(log_retention_days) FROM system_settings), 0), 400)),
    current_date + 7);

INSERT INTO log_entries (id, source_id, group_id, severity, message, parsed_message, stack_trace, raw_line,
                         namespace, pod_name, container_name, service_name, cluster, timestamp, created_at)
SELECT id, source_id, group_id, severity, message, parsed_message, stack_trace, raw_line,
       namespace, pod_name, container_name, service_name, cluster, timestamp, created_at
FROM log_entries_unpartitioned;

-- The id sequence outlives the old table.
ALTER SEQUENCE log_entries_id_seq OWNED BY log_entries.id;
DROP TABLE log_entries_unpartitioned;

-- Same indexes as before (created on every partition, current and future).
CREATE INDEX idx_log_entries_timestamp ON log_entries (timestamp DESC);
CREATE INDEX idx_log_entries_severity ON log_entries (severity);
CREATE INDEX idx_log_entries_group ON log_entries (group_id);
CREATE INDEX idx_log_entries_source_ts ON log_entries (source_id, timestamp DESC);
CREATE INDEX idx_log_entries_message_trgm ON log_entries USING gin (lower(message) gin_trgm_ops);
CREATE INDEX idx_log_entries_raw_line_trgm ON log_entries USING gin (lower(raw_line) gin_trgm_ops);

ANALYZE log_entries;
