-- V2__Seed_expired_batches.sql
-- Insert a test medication + expired batches so the Expired History feature has data to display.
-- All inserts are conditional to avoid conflicts with existing data.

DO $$
DECLARE
    med_id BIGINT;
BEGIN
    -- Reuse the first existing medication, or insert a new one
    SELECT medication_id INTO med_id FROM medications ORDER BY medication_id LIMIT 1;

    IF med_id IS NULL THEN
        INSERT INTO medications (name, form, pediatric_dosage)
        VALUES ('Amoxicillin', 'capsule', '25mg/kg/day')
        RETURNING medication_id INTO med_id;
    END IF;
    -- Insert expired batches only if they don't already exist
    INSERT INTO batches (batch_number, manufacture_date, expiration_date, quantity, medication_id)
    SELECT 'EXP-BATCH-001', '2023-01-01', '2024-06-30', 100, med_id
    WHERE NOT EXISTS (SELECT 1 FROM batches WHERE batch_number = 'EXP-BATCH-001');

    INSERT INTO batches (batch_number, manufacture_date, expiration_date, quantity, medication_id)
    SELECT 'EXP-BATCH-002', '2023-06-01', '2025-01-15', 50, med_id
    WHERE NOT EXISTS (SELECT 1 FROM batches WHERE batch_number = 'EXP-BATCH-002');

    INSERT INTO batches (batch_number, manufacture_date, expiration_date, quantity, medication_id)
    SELECT 'EXP-BATCH-003', '2022-03-01', '2023-12-31', 200, med_id
    WHERE NOT EXISTS (SELECT 1 FROM batches WHERE batch_number = 'EXP-BATCH-003');
END $$;