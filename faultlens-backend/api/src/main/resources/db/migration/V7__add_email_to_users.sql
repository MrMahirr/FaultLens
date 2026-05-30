ALTER TABLE users ADD COLUMN email VARCHAR(255);

UPDATE users SET email = 'admin@faultlens.com' WHERE username = 'admin';
