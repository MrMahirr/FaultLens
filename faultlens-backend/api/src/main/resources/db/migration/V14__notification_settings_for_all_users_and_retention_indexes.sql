-- V8 created notification settings only for user_id = 1, assuming that is the admin. Give every
-- user that has none the defaults, whatever its id (NotificationSettingsService also creates
-- them on first use). V8 itself is left unchanged: Flyway checksums applied migrations.
INSERT INTO notification_settings (user_id, email_enabled, slack_enabled, push_enabled, mobile_enabled)
SELECT u.id, true, false, false, false
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM notification_settings s WHERE s.user_id = u.id);

-- LogRetentionJob deletes by these columns in batches.
CREATE INDEX IF NOT EXISTS idx_analyses_analyzed_at ON analyses (analyzed_at);
CREATE INDEX IF NOT EXISTS idx_alarms_triggered_at ON alarms (triggered_at);
