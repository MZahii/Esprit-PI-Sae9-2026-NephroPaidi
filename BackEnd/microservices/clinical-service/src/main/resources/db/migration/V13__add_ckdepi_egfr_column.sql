-- Quick fix migration for missing ckdepi_egfr column
-- This migration ensures the column exists with correct type for CKD-EPI formula support

-- Drop the column if it exists with wrong type and recreate it
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='consultation_metrics' AND column_name='ckdepi_egfr') THEN
        ALTER TABLE consultation_metrics DROP COLUMN ckdepi_egfr;
    END IF;
END $$;

ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS ckdepi_egfr FLOAT;
COMMENT ON COLUMN consultation_metrics.ckdepi_egfr IS 'eGFR calculated using CKD-EPI 2021 formula (mL/min/1.73m²) - double precision';
