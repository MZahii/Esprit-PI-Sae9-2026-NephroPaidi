ALTER TABLE consultation_metrics
    ADD COLUMN IF NOT EXISTS systolic_bp_mmhg NUMERIC,
    ADD COLUMN IF NOT EXISTS diastolic_bp_mmhg NUMERIC,
    ADD COLUMN IF NOT EXISTS heart_rate_bpm INTEGER,
    ADD COLUMN IF NOT EXISTS respiratory_rate_bpm INTEGER,
    ADD COLUMN IF NOT EXISTS temperature_c NUMERIC,
    ADD COLUMN IF NOT EXISTS oxygen_saturation_pct INTEGER;
