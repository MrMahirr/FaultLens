-- P10 / 2.1–2.2: validate the constraints V23 and V24 added NOT VALID.
-- VALIDATE CONSTRAINT scans log_groups under SHARE UPDATE EXCLUSIVE, which does not block reads
-- or the processor's INSERT/UPDATE; doing it in V23/V24 would have scanned under the ACCESS
-- EXCLUSIVE lock of their ALTER TABLE. Runs outside a transaction (V25…sql.conf) so each
-- statement takes and releases its own lock.
ALTER TABLE log_groups VALIDATE CONSTRAINT log_groups_status_check;
ALTER TABLE log_groups VALIDATE CONSTRAINT log_groups_merged_into_fk;
ALTER TABLE log_groups VALIDATE CONSTRAINT log_groups_not_merged_into_self;
