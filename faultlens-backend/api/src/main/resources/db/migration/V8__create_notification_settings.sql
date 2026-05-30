CREATE TABLE notification_settings (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) UNIQUE,
    email_enabled   BOOLEAN DEFAULT true,
    slack_enabled   BOOLEAN DEFAULT false,
    slack_webhook   VARCHAR(500),
    push_enabled    BOOLEAN DEFAULT false,
    mobile_enabled  BOOLEAN DEFAULT false,
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Default settings for admin user
INSERT INTO notification_settings (user_id, email_enabled, slack_enabled, push_enabled, mobile_enabled)
VALUES (1, true, false, false, false);
