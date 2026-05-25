CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(100) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(50)  DEFAULT 'ADMIN',
    created_at TIMESTAMPTZ  DEFAULT NOW()
);

-- password: admin123 (BCrypt hash)
INSERT INTO users (username, password, role)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN');
