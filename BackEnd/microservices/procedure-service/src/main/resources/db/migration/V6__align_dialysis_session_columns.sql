-- V3__add_idempotency_key_to_dialysis_sessions.sql
-- Adds idempotency key field for duplicate request detection
-- This prevents creating duplicate dialysis sessions from duplicate API calls

ALTER TABLE dialysis_sessions ADD COLUMN idempotency_key VARCHAR(255) UNIQUE;
CREATE INDEX idx_dialysis_sessions_idempotency_key ON dialysis_sessions(idempotency_key);
