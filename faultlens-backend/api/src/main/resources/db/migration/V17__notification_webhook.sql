-- Generic webhook channel (P07). EmailJS columns are no longer used (alarm e-mails go over SMTP);
-- they are left in place so existing rows are untouched and can be dropped in a later cleanup.
ALTER TABLE notification_settings ADD COLUMN webhook_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE notification_settings ADD COLUMN webhook_url VARCHAR(500);
