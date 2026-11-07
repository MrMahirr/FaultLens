-- P08: substring search on log text. LogQueryService filters with lower(col) LIKE '%term%';
-- trigram GIN indexes on exactly those expressions let Postgres answer it without a full scan
-- (bench, 1M rows: rare term p95 1188 ms -> 32 ms, absent term 742 ms -> 21 ms).
-- Non-transactional (see .sql.conf) so the indexes are built CONCURRENTLY: writes keep flowing.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_log_entries_message_trgm
    ON log_entries USING gin (lower(message) gin_trgm_ops);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_log_entries_raw_line_trgm
    ON log_entries USING gin (lower(raw_line) gin_trgm_ops);
-- Statistics for the new expressions; without them the planner misjudges LIKE selectivity and
-- walks the timestamp index instead (measured: 645 ms instead of 2 ms).
ANALYZE log_entries;
