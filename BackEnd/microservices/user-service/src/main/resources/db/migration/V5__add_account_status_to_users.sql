ALTER TABLE users
    ADD COLUMN IF NOT EXISTS account_status VARCHAR(50);

UPDATE users
SET account_status = CASE
                         WHEN enabled = true THEN 'ACTIVE'
                         ELSE 'PENDING_CONTRACT'
    END
WHERE account_status IS NULL;

ALTER TABLE users
    ALTER COLUMN account_status SET NOT NULL;