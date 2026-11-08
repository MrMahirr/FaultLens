-- P10 / 2.3 benchmark: log context (GET /logs/{id}/context) on a large partitioned log_entries.
-- Runs in a throwaway PostgreSQL 18 (NOT the app database: it creates its own log_entries):
--
--   docker run -d --name faultlens-ctx-bench -e POSTGRES_PASSWORD=bench --shm-size=1g postgres:18-alpine
--   docker cp bench/context.sql faultlens-ctx-bench:/context.sql
--   docker exec faultlens-ctx-bench psql -U postgres -v rows=50000000 -v days=30 -f /context.sql
--   (again on the loaded data: add -v load=0; on Git Bash set MSYS_NO_PATHCONV=1)
--   docker rm -f faultlens-ctx-bench
--
-- Same table shape, daily partitions and indexes as V19 + V26 (trigram indexes left out: they
-- only slow the load and play no part in this query). Measures what LogContextService does per
-- request: find the entry by id (all partitions' primary keys), then the two LIMIT 51 walks.
\set ON_ERROR_STOP on
\timing on
-- -v load=0: measure again on the data already loaded.
\if :{?load}
\else
\set load 1
\endif
\if :load

DROP TABLE IF EXISTS log_entries CASCADE;
CREATE TABLE log_entries (
    id             BIGINT       NOT NULL,
    source_id      BIGINT,
    group_id       BIGINT,
    severity       VARCHAR(20)  NOT NULL,
    message        TEXT         NOT NULL,
    pod_name       VARCHAR(255),
    service_name   VARCHAR(255),
    timestamp      TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (id, timestamp)
) PARTITION BY RANGE (timestamp);
CREATE TABLE log_entries_default PARTITION OF log_entries DEFAULT;
SELECT format('CREATE TABLE log_entries_%s PARTITION OF log_entries FOR VALUES FROM (%L) TO (%L)',
              to_char(d, 'YYYYMMDD'), d, d + 1)
  FROM generate_series(current_date - :days, current_date, interval '1 day') AS g(d0),
       LATERAL (SELECT g.d0::date AS d) x
\gexec

-- 3 sources: 1 and 2 are Kubernetes with 20 pods each, 3 has no pod (local file / HTTP ingest).
-- Rows are spread evenly over :days days; ids follow insertion order (like the sequence).
INSERT INTO log_entries (id, source_id, severity, message, pod_name, service_name, timestamp)
SELECT g,
       1 + (g % 3),
       (ARRAY['INFO','INFO','INFO','INFO','DEBUG','WARN','ERROR'])[1 + (g % 7)],
       'GET /api/v1/orders/' || (g % 100000) || ' 200 in ' || (g % 900) || 'ms',
       CASE WHEN g % 3 = 2 THEN NULL ELSE 'pod-' || ((g / 3) % 20) END,
       'svc-' || (g % 3),
       now() - make_interval(days => :days) + (g::double precision / :rows) * make_interval(days => :days)
  FROM generate_series(1, :rows) g;

CREATE INDEX idx_log_entries_timestamp ON log_entries (timestamp DESC);
CREATE INDEX idx_log_entries_source_ts ON log_entries (source_id, timestamp DESC);
CREATE INDEX idx_log_entries_context ON log_entries (source_id, pod_name, timestamp, id);
VACUUM ANALYZE log_entries;
\endif

SELECT pg_size_pretty(sum(pg_total_relation_size(relid))) AS table_with_indexes,
       pg_size_pretty(sum(pg_indexes_size(relid)) FILTER (WHERE relid IS NOT NULL)) AS indexes,
       count(*) FILTER (WHERE isleaf) AS partitions
  FROM pg_partition_tree('log_entries');
SELECT pg_size_pretty(sum(pg_relation_size(c.oid))) AS context_index
  FROM pg_inherits i JOIN pg_class c ON c.oid = i.inhrelid
 WHERE i.inhparent = 'idx_log_entries_context'::regclass;

-- One request, explained (an entry of a Kubernetes pod in the middle of the data).
SELECT id AS anchor_id, timestamp AS anchor_ts, source_id, pod_name
  FROM log_entries WHERE id = (:rows / 2)::bigint \gset
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF)
SELECT * FROM log_entries
 WHERE source_id = :source_id AND pod_name = :'pod_name'
   AND (timestamp, id) < (:'anchor_ts'::timestamptz, :anchor_id)
   AND timestamp BETWEEN :'anchor_ts'::timestamptz - interval '60 minutes' AND :'anchor_ts'::timestamptz
 ORDER BY timestamp DESC, id DESC LIMIT 51;

-- 300 random entries, each timed as the service does it.
CREATE OR REPLACE FUNCTION pg_temp.context_once(entry bigint) RETURNS double precision LANGUAGE plpgsql AS $$
DECLARE
    started timestamptz := clock_timestamp();
    a record;
    n int;
BEGIN
    SELECT * INTO a FROM log_entries WHERE id = entry;
    IF a.pod_name IS NULL THEN
        EXECUTE 'SELECT count(*) FROM (SELECT 1 FROM log_entries WHERE source_id = $1 AND pod_name IS NULL
                 AND (timestamp, id) < ($2, $3) AND timestamp BETWEEN $2 - interval ''60 minutes'' AND $2
                 ORDER BY timestamp DESC, id DESC LIMIT 51) s' INTO n USING a.source_id, a.timestamp, a.id;
        EXECUTE 'SELECT count(*) FROM (SELECT 1 FROM log_entries WHERE source_id = $1 AND pod_name IS NULL
                 AND (timestamp, id) > ($2, $3) AND timestamp BETWEEN $2 AND $2 + interval ''60 minutes''
                 ORDER BY timestamp, id LIMIT 51) s' INTO n USING a.source_id, a.timestamp, a.id;
    ELSE
        EXECUTE 'SELECT count(*) FROM (SELECT 1 FROM log_entries WHERE source_id = $1 AND pod_name = $4
                 AND (timestamp, id) < ($2, $3) AND timestamp BETWEEN $2 - interval ''60 minutes'' AND $2
                 ORDER BY timestamp DESC, id DESC LIMIT 51) s' INTO n USING a.source_id, a.timestamp, a.id, a.pod_name;
        EXECUTE 'SELECT count(*) FROM (SELECT 1 FROM log_entries WHERE source_id = $1 AND pod_name = $4
                 AND (timestamp, id) > ($2, $3) AND timestamp BETWEEN $2 AND $2 + interval ''60 minutes''
                 ORDER BY timestamp, id LIMIT 51) s' INTO n USING a.source_id, a.timestamp, a.id, a.pod_name;
    END IF;
    RETURN extract(epoch FROM clock_timestamp() - started) * 1000;
END;
$$;

\timing off
WITH runs AS (
    SELECT pg_temp.context_once(1 + floor(random() * :rows)::bigint) AS ms FROM generate_series(1, 300)
)
SELECT count(*) AS requests,
       round(percentile_cont(0.50) WITHIN GROUP (ORDER BY ms)::numeric, 2) AS p50_ms,
       round(percentile_cont(0.95) WITHIN GROUP (ORDER BY ms)::numeric, 2) AS p95_ms,
       round(max(ms)::numeric, 2) AS max_ms
  FROM runs;
