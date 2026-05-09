-- Add idempotency_key column to dialysis_sessions for request deduplication
-- This ensures we can prevent duplicate procedure requests
ALTER TABLE dialysis_sessions
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255) UNIQUE;
