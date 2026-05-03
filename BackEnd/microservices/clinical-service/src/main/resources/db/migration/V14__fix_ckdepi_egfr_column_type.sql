-- Migration V14: Fix ckdepi_egfr column type
-- The previous migration created it as DECIMAL but Hibernate expects FLOAT

-- Drop and recreate with correct type
ALTER TABLE consultation_metrics DROP COLUMN IF EXISTS ckdepi_egfr;

ALTER TABLE consultation_metrics ADD COLUMN ckdepi_egfr FLOAT;
COMMENT ON COLUMN consultation_metrics.ckdepi_egfr IS 'eGFR calculated using CKD-EPI 2021 formula (mL/min/1.73m²)';
