CREATE TABLE alarms (
    id BIGSERIAL PRIMARY KEY,
    rule_id VARCHAR(255),
    rule_name VARCHAR(255) NOT NULL,
    source_id BIGINT,
    severity VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    message TEXT,
    triggered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE
);
