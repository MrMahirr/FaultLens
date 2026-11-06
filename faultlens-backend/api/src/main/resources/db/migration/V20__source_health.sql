-- P08: collector health per source, shared by all instances (the one holding the source's
-- lease writes it, on state changes only).
--   status: CONNECTED (collecting), ERROR (adapter failing, see last_error), IDLE (not collected)
ALTER TABLE log_sources ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'IDLE'
    CHECK (status IN ('CONNECTED', 'ERROR', 'IDLE'));
ALTER TABLE log_sources ADD COLUMN last_error TEXT;
ALTER TABLE log_sources ADD COLUMN last_error_at TIMESTAMPTZ;
ALTER TABLE log_sources ADD COLUMN collector_instance VARCHAR(100);
