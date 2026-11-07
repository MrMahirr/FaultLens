# ADR 0003 — Scaling and observability (P08)

- Status: accepted
- Date: 2026-11-06
- Related: ADR 0001 (alarm rules), ADR 0002 (channels, sessions)

## Measure first

`bench/` holds an isolated stack with its own project name, ports and volumes. It never touches development data.

- `seed.sql` loads 1M realistic rows.
- `search.py` measures search latency (p50/p95 of the queries the UI sends).
- `ingest.py` measures Kafka throughput and consumer lag.
- `multi_instance.py` checks collection with two backend instances.

All numbers below come from the same machine.

| | before | after |
|---|---|---|
| ingest, 100k events, 3 consumers | 128 events/s, 3.4% lost to the DLQ | 1733 events/s, 0 lost |
| search p95, rare term, whole table | 1188 ms | 70 ms |
| search p95, absent term | 742 ms | 32 ms |
| search p95, common term, last 24 h | 433 ms | 78 ms |
| 2 instances, 1 source, 2×2000 lines | (would double) | 0 duplicates, failover 41 s |

## Decisions

### Grouping

The fingerprint is built from the normalized message: numbers, ids, IPs, UUIDs, e-mails and timestamps become placeholders. Groups are upserted once per batch, in fingerprint order, with `INSERT … ON CONFLICT DO UPDATE`.

- Before this, about 95% of groups held a single entry.
- The old per-entry `SELECT … FOR UPDATE` deadlocked between consumers. Postgres then aborted the transaction, so the rest of the batch was sent to the DLQ.

### Search

- `pg_trgm` GIN indexes on `lower(message)` and `lower(raw_line)` match the predicate the API builds. The migration runs ANALYZE, because without expression statistics the planner chose the wrong plan.
- The list endpoint is always time-bounded: the last 24 h unless the caller passes `from`/`to`. The UI offers 1 h / 24 h / 7 d / 30 d / all.
- Cost: GIN maintenance lowers ingest from about 2100 to about 1700 events/s.

### Partitioning

`log_entries` is RANGE-partitioned by UTC day (V19), with a default partition for rows outside the range.

- `faultlens_ensure_log_partitions` creates days ahead. It skips any day whose rows already sit in the default partition.
- Retention drops whole expired days with `DROP TABLE` (lock_timeout 5 s), then batch-deletes the remainder.
- The FK `analyses.log_entry_id` was dropped; the value is kept.
- Flyway uses a session lock, because `CREATE INDEX CONCURRENTLY` waits forever on the transactional one.
- ClickHouse/OpenSearch stay the next step if one Postgres no longer suffices (see Ö02).

### Collector leases

Each source has a Redis lease: `SET NX PX 30 s`.

- Renew and release are owner-only (Lua).
- Reconcile runs every 15 s and heartbeat every 10 s. An edit takes the lease over; disable/delete revokes it.
- Health (`status`, `last_error`, `collector_instance`) lives in `log_sources` and is written on transitions only.
- Without Redis the collector fails open: it keeps collecting, so duplicates are possible with several instances.
- Known gap: LOCAL_FILE starts at the end of the file, so lines written while no instance holds the lease are not read.

### Metrics

- Exposed via Micrometer and Prometheus.
- `/actuator/prometheus` needs no credentials only on `MANAGEMENT_PORT`, which is unpublished. On the main port it is ADMIN-only.
- Custom metrics: `faultlens_logs_ingested_total`, `faultlens_logs_dlq_total`, `faultlens_analysis_duration_seconds`, `faultlens_ai_tokens_total`, `faultlens_alarms_fired_total`, `faultlens_kafka_consumer_lag`.
- `faultlens_kafka_consumer_lag` is end offset minus committed offset, read via the AdminClient. The client's `records_lag_max` is NaN while idle.
- `/livez` and `/readyz` stay on the main port for health checks.

### Tracing

- OpenTelemetry with OTLP export, off by default.
- Only HTTP requests and Kafka operations may start a trace.
- Scheduled jobs, Redis heartbeats and scrape requests would otherwise be nearly all spans. This is filtered both by an `ObservationPredicate` and by a `SpanExportingPredicate`.
- Batch listeners (processor) start a new trace.

### Production

The backend image:
- builds with a dependency layer cache;
- runs as a non-root user;
- has a `HEALTHCHECK` on `/readyz`;
- sets `MaxRAMPercentage=75`.

Around it:
- Docker sources go through a read-only `docker-socket-proxy` (containers only, no POST).
- Nginx sends HSTS, a CSP and Referrer-Policy.
- A Helm chart deploys 2 backend replicas, with a PodDisruptionBudget and an optional ServiceMonitor.
- semantic-release creates the release and pushes images to GHCR after CI is green on master.
- A daily `pg_dump` runs, its result is verified with `pg_restore --list`, and dumps are kept 14 days.
