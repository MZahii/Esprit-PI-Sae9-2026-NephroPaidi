-- V11__add_appointment_timestamps.sql
-- Add started_at and completed_at timestamps to appointment table for appointment start logic

ALTER TABLE appointment ADD COLUMN IF NOT EXISTS started_at TIMESTAMP NULL;
ALTER TABLE appointment ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_appointment_started_at ON appointment(started_at);
CREATE INDEX IF NOT EXISTS idx_appointment_completed_at ON appointment(completed_at);
