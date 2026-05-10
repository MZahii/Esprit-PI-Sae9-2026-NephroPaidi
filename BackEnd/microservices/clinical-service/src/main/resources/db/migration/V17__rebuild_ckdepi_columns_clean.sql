-- Migration V17: Clean rebuild of CKD-EPI columns
-- This migration discards previous attempts and properly creates all necessary columns with correct types

-- Remove columns if they exist (without checking dependencies)
DO $$
DECLARE
    col_name TEXT;
BEGIN
    FOREACH col_name IN ARRAY ARRAY['ckdepi_egfr', 'creatinine_umol', 'previous_egfr', 'egfr_change', 'egfr_change_percent', 'egfr_last_updated_at', 'serum_creatinine_unit', 'egfr_formula_used', 'egfr_quality_indicator', 'egfr_trend', 'lab_scr_reference_range', 'serum_creatinine_lab_id']
    LOOP
        BEGIN
            EXECUTE 'ALTER TABLE consultation_metrics DROP COLUMN IF EXISTS ' || col_name;
        EXCEPTION WHEN OTHERS THEN
            -- Ignore if column doesn't exist or has dependencies
            NULL;
        END;
    END LOOP;
END $$;

-- Add all columns with CORRECT types (DOUBLE PRECISION for numeric Double fields)
ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS ckdepi_egfr DOUBLE PRECISION;
ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS creatinine_umol DOUBLE PRECISION;
ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS serum_creatinine_unit VARCHAR(20) DEFAULT 'MICROMOL_L';
ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS egfr_formula_used VARCHAR(50) DEFAULT 'CKD_EPI_2021';
ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS egfr_quality_indicator VARCHAR(50);
ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS previous_egfr DOUBLE PRECISION;
ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS egfr_change DOUBLE PRECISION;
ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS egfr_change_percent DOUBLE PRECISION;
ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS egfr_trend VARCHAR(50);
ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS egfr_last_updated_at TIMESTAMP;
ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS lab_scr_reference_range VARCHAR(50);
ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS serum_creatinine_lab_id UUID;

-- Add comments
COMMENT ON COLUMN consultation_metrics.ckdepi_egfr IS 'eGFR calculated using CKD-EPI 2021 formula (mL/min/1.73m²)';
COMMENT ON COLUMN consultation_metrics.creatinine_umol IS 'Serum creatinine in SI units (µmol/L) - European standard';
COMMENT ON COLUMN consultation_metrics.previous_egfr IS 'Previous eGFR value for trend analysis';
COMMENT ON COLUMN consultation_metrics.egfr_change IS 'Absolute change in eGFR (mL/min/1.73m²)';
COMMENT ON COLUMN consultation_metrics.egfr_change_percent IS 'Percentage change in eGFR from previous';
