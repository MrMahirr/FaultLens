-- Roles (P07): ADMIN manages users, settings and deletes; EDITOR edits sources/rules and handles
-- alarms; VIEWER reads. Existing accounts keep their role (they were all ADMIN).
UPDATE users SET role = 'ADMIN' WHERE role IS NULL OR role NOT IN ('ADMIN', 'EDITOR', 'VIEWER');
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'VIEWER';
ALTER TABLE users ALTER COLUMN role SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'EDITOR', 'VIEWER'));

-- Deleting a user removes its notification settings too.
ALTER TABLE notification_settings DROP CONSTRAINT IF EXISTS notification_settings_user_id_fkey;
ALTER TABLE notification_settings
    ADD CONSTRAINT notification_settings_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
