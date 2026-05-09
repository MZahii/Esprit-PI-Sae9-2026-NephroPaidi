ALTER TABLE dialysis_sessions
    ADD COLUMN IF NOT EXISTS patient_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS consultation_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS appointment_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

UPDATE dialysis_sessions
SET created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW())
WHERE created_at IS NULL
   OR updated_at IS NULL;

ALTER TABLE dialysis_sessions
    ALTER COLUMN created_at SET NOT NULL;
