CREATE TABLE deployments (
    id           BIGSERIAL PRIMARY KEY,
    service_name VARCHAR(255) NOT NULL,
    version      VARCHAR(100),
    environment  VARCHAR(100),
    deployed_at  TIMESTAMPTZ  NOT NULL,
    deployed_by  VARCHAR(255),
    status       VARCHAR(50)  DEFAULT 'SUCCESS',
    notes        TEXT
);
CREATE INDEX idx_deployments_deployed_at ON deployments(deployed_at DESC);
CREATE INDEX idx_deployments_service     ON deployments(service_name);
