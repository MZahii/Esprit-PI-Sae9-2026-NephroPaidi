ALTER TABLE surgical_cases
    ADD COLUMN IF NOT EXISTS surgery_request_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_surgical_cases_surgery_request_id'
    ) THEN
        ALTER TABLE surgical_cases
            ADD CONSTRAINT uq_surgical_cases_surgery_request_id UNIQUE (surgery_request_id);
    END IF;
END $$;
