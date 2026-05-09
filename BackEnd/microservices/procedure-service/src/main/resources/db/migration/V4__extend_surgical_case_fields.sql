ALTER TABLE surgical_cases
    ADD COLUMN IF NOT EXISTS consultation_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS appointment_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

UPDATE surgical_cases
SET created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW())
WHERE created_at IS NULL
   OR updated_at IS NULL;

ALTER TABLE surgical_cases
    ALTER COLUMN created_at SET NOT NULL;
