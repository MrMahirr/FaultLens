-- Synthetic log data for the P08 benchmark (bench stack only!).
-- psql -v rows=1000000 -v days=30 -f bench/seed.sql
-- Realistic shape: 3 sources, ~300 error groups, 30 days, INFO-heavy severity mix, varied text.
\set ON_ERROR_STOP on
\timing on

-- Re-runnable: the bench database holds generated data only.
TRUNCATE log_entries CASCADE;

INSERT INTO log_sources (name, type, config, enabled)
SELECT n, t, '{}'::jsonb, false
FROM (VALUES ('orders-api', 'DOCKER'), ('payments-svc', 'KUBERNETES'), ('edge-nginx', 'LOCAL_FILE')) AS s(n, t)
WHERE NOT EXISTS (SELECT 1 FROM log_sources WHERE name = s.n);

CREATE TEMP TABLE tpl(i int, severity text, msg text);
INSERT INTO tpl VALUES
 (0, 'INFO',     'GET /api/v1/orders/{n} 200 in {ms}ms'),
 (1, 'INFO',     'POST /api/v1/payments 201 in {ms}ms user={u}'),
 (2, 'INFO',     'Cache hit ratio {n}% for region eu-{r}'),
 (3, 'INFO',     'Scheduled job cleanup-{r} finished in {ms}ms'),
 (4, 'DEBUG',    'Loaded {n} rows from inventory partition {r}'),
 (5, 'INFO',     'User {u} logged in from 10.0.{r}.{n}'),
 (6, 'WARN',     'Slow query on orders took {ms}ms (threshold 500ms)'),
 (7, 'WARN',     'Retrying request to payment-gateway attempt {r}/3'),
 (8, 'WARN',     'Connection pool usage at {n}% (HikariPool-{r})'),
 (9, 'ERROR',    'Connection refused: db-{r}.internal:5432 after {ms}ms'),
 (10, 'ERROR',   'java.lang.NullPointerException: Cannot invoke "Order.getId()" because order is null (OrderService.java:{n})'),
 (11, 'ERROR',   'Timeout waiting for lock on account {u} after {ms}ms'),
 (12, 'ERROR',   'upstream timed out (110: Connection timed out) while reading response header from upstream, client: 172.18.{r}.{n}'),
 (13, 'CRITICAL','OutOfMemoryError: Java heap space in worker-{r}'),
 (14, 'ERROR',   'Payment {u} declined by provider: insufficient_funds');

-- Groups for the error templates (fingerprint per template x variant).
INSERT INTO log_groups (fingerprint, first_message, severity, count, source_id, first_seen_at, last_seen_at)
SELECT md5('bench-' || t.i || '-' || v), t.msg, t.severity, 1,
       (SELECT id FROM log_sources ORDER BY id LIMIT 1 OFFSET (v % 3)),
       now() - make_interval(days => :days), now()
FROM tpl t CROSS JOIN generate_series(0, 49) v
WHERE t.severity IN ('ERROR', 'CRITICAL', 'WARN')
ON CONFLICT (fingerprint) DO NOTHING;

-- Weighted template pick: ~70% INFO/DEBUG, ~15% WARN, ~15% ERROR/CRITICAL.
CREATE TEMP TABLE pick AS
SELECT g::bigint AS k,
       CASE WHEN r < 0.70 THEN floor(random() * 6)::int
            WHEN r < 0.85 THEN 6 + floor(random() * 3)::int
            WHEN r < 0.995 THEN 9 + (ARRAY[0,1,2,3,5])[1 + floor(random() * 5)::int]
            ELSE 13 END AS i,
       now() - random() * make_interval(days => :days) AS ts
FROM (SELECT g, random() AS r FROM generate_series(1, :rows) g) x;

INSERT INTO log_entries (source_id, group_id, severity, message, raw_line, service_name, timestamp)
SELECT src.id,
       CASE WHEN t.severity IN ('ERROR', 'CRITICAL', 'WARN')
            THEN (SELECT id FROM log_groups WHERE fingerprint = md5('bench-' || t.i || '-' || (p.k % 50))) END,
       t.severity, m.msg,
       to_char(p.ts, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') || ' ' || t.severity || ' ' || m.msg,
       src.name, p.ts
FROM pick p
JOIN tpl t ON t.i = p.i
CROSS JOIN LATERAL (
  SELECT replace(replace(replace(replace(replace(t.msg,
           '{n}', (p.k % 997)::text),
           '{ms}', (5 + (p.k * 7919) % 4000)::text),
           '{u}', 'u' || (p.k * 104729 % 50000)::text),
           '{r}', (p.k % 13)::text),
           '{x}', '') AS msg) m
JOIN LATERAL (SELECT id, name FROM log_sources ORDER BY id LIMIT 1 OFFSET (p.k % 3)) src ON true;

ANALYZE log_entries;
SELECT count(*) AS rows, pg_size_pretty(pg_total_relation_size('log_entries')) AS size FROM log_entries;
