-- Users created by bootstrap (or whose password was reset because it was a
-- well-known default) must choose a new password before using the API.
ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
