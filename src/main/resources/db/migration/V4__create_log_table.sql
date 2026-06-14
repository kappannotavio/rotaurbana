CREATE TABLE app_log (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    description TEXT,
    performed_by_id BIGINT,
    performed_by_name VARCHAR(255),
    timestamp TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_log_timestamp ON app_log(timestamp DESC);
CREATE INDEX idx_log_entity_type ON app_log(entity_type);
