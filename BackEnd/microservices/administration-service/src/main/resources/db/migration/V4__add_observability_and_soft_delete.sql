ALTER TABLE staff_contracts
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE staff_contracts
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

ALTER TABLE staff_contracts
    ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(120);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(80) NOT NULL,
    entity_id BIGINT NOT NULL,
    scope_id BIGINT,
    action VARCHAR(80) NOT NULL,
    actor VARCHAR(160),
    old_value TEXT,
    new_value TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_entity_scope_created_at
    ON audit_logs (entity_type, scope_id, created_at DESC);

CREATE TABLE IF NOT EXISTS domain_event_logs (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    payload TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(120) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    target_user_id BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_target_user_created_at
    ON notifications (target_user_id, created_at DESC);
