-- P10 / 1.2: a logs:ingest key is bound to one HTTP source. Logs pushed with the key are stored
-- under that source; the client cannot choose another one. Deleting the source leaves the key
-- (for auditing) without a source, and ingest with it is then refused.
ALTER TABLE api_keys ADD COLUMN source_id BIGINT REFERENCES log_sources (id) ON DELETE SET NULL;
