-- Migration Script: Add CKD-EPI Formula Support and Enhanced Metrics Tracking
-- Version: V12__add_ckdepi_formula_support.sql
-- Date: 2026-05-01
-- Purpose: Add European CKD-EPI formula support with SI units (µmol/L)

-- ============================================================================
-- 1. Update consultation_metrics table with new columns
-- ============================================================================

ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS ckdepi_egfr DECIMAL(10, 2);
COMMENT ON COLUMN consultation_metrics.ckdepi_egfr IS 'eGFR calculated using CKD-EPI 2021 formula (mL/min/1.73m²)';

ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS creatinine_umol DECIMAL(10, 2);
COMMENT ON COLUMN consultation_metrics.creatinine_umol IS 'Serum creatinine in SI units (µmol/L) - European standard';

ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS serum_creatinine_unit VARCHAR(20) DEFAULT 'MICROMOL_L';
COMMENT ON COLUMN consultation_metrics.serum_creatinine_unit IS 'Unit of serum creatinine measurement: MICROMOL_L (SI) or MG_DL (legacy)';

ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS egfr_formula_used VARCHAR(50) DEFAULT 'CKD_EPI_2021';
COMMENT ON COLUMN consultation_metrics.egfr_formula_used IS 'Formula used for eGFR calculation: CKD_EPI_2021 (European standard)';

ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS egfr_quality_indicator VARCHAR(50);
COMMENT ON COLUMN consultation_metrics.egfr_quality_indicator IS 'Data quality indicator: HIGH_QUALITY, MODERATE, LOW_QUALITY';

ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS previous_egfr DECIMAL(10, 2);
COMMENT ON COLUMN consultation_metrics.previous_egfr IS 'Previous eGFR value for trend analysis';

ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS egfr_change DECIMAL(10, 2);
COMMENT ON COLUMN consultation_metrics.egfr_change IS 'Absolute change in eGFR (current - previous) in mL/min/1.73m²';

ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS egfr_change_percent DECIMAL(10, 2);
COMMENT ON COLUMN consultation_metrics.egfr_change_percent IS 'Percentage change in eGFR from previous result';

ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS egfr_trend VARCHAR(50);
COMMENT ON COLUMN consultation_metrics.egfr_trend IS 'Trend status: STABLE, DECLINING, RAPID_DECLINE, IMPROVING, RAPID_IMPROVEMENT, NO_PREVIOUS_DATA';

ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS egfr_last_updated_at TIMESTAMP;
COMMENT ON COLUMN consultation_metrics.egfr_last_updated_at IS 'When eGFR was last calculated/updated';

ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS lab_scr_reference_range VARCHAR(50);
COMMENT ON COLUMN consultation_metrics.lab_scr_reference_range IS 'Laboratory reference range for serum creatinine (e.g., "60-120 µmol/L")';

ALTER TABLE consultation_metrics ADD COLUMN IF NOT EXISTS serum_creatinine_lab_id UUID;
COMMENT ON COLUMN consultation_metrics.serum_creatinine_lab_id IS 'Reference to lab_result.id for audit trail';

-- ============================================================================
-- 2. Create lab_result table for storing individual lab test results
-- ============================================================================

CREATE TABLE IF NOT EXISTS lab_result (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lab_request_id UUID,
    patient_id BIGINT NOT NULL,
    test_name VARCHAR(255) NOT NULL,
    test_code VARCHAR(50),
    value DECIMAL(15, 4) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    reference_range_min DECIMAL(15, 4),
    reference_range_max DECIMAL(15, 4),
    is_abnormal BOOLEAN DEFAULT FALSE,
    interpretation VARCHAR(50),
    lab_timestamp TIMESTAMP NOT NULL,
    received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add foreign keys only if referenced tables exist
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'lab_request') THEN
        ALTER TABLE lab_result ADD CONSTRAINT fk_lab_result_request 
            FOREIGN KEY (lab_request_id) REFERENCES lab_request(id) ON DELETE CASCADE;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'patient_profile') THEN
        ALTER TABLE lab_result ADD CONSTRAINT fk_lab_result_patient 
            FOREIGN KEY (patient_id) REFERENCES patient_profile(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lab_result_request ON lab_result(lab_request_id);
CREATE INDEX IF NOT EXISTS idx_lab_result_patient ON lab_result(patient_id);
CREATE INDEX IF NOT EXISTS idx_lab_result_test_name ON lab_result(test_name);
CREATE INDEX IF NOT EXISTS idx_lab_result_timestamp ON lab_result(lab_timestamp);

COMMENT ON TABLE lab_result IS 'Individual lab test results (serum creatinine, BUN, electrolytes, etc)';

-- ============================================================================
-- 3. Create egfr_calculation_audit table for regulatory compliance
-- ============================================================================

CREATE TABLE IF NOT EXISTS egfr_calculation_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consultation_metrics_id UUID,
    patient_id BIGINT NOT NULL,
    
    -- Input parameters
    input_scr DECIMAL(10, 2) NOT NULL,
    input_scr_unit VARCHAR(20) NOT NULL,
    input_age INT NOT NULL,
    input_sex VARCHAR(1) NOT NULL,
    
    -- Calculation details
    calculated_egfr DECIMAL(10, 2) NOT NULL,
    calculated_ckd_stage VARCHAR(20),
    formula_used VARCHAR(50) NOT NULL,
    formula_version VARCHAR(20),
    
    -- Quality metrics
    scr_quality_flag VARCHAR(50),
    has_previous_egfr BOOLEAN DEFAULT FALSE,
    previous_egfr DECIMAL(10, 2),
    egfr_trend VARCHAR(50),
    
    -- Audit trail
    calculated_by_service VARCHAR(100),
    calculation_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    triggered_by_event VARCHAR(100)
);

-- Add foreign keys conditionally
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'consultation_metrics') THEN
        ALTER TABLE egfr_calculation_audit ADD CONSTRAINT fk_audit_metrics 
            FOREIGN KEY (consultation_metrics_id) REFERENCES consultation_metrics(id) ON DELETE SET NULL;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'patient_profile') THEN
        ALTER TABLE egfr_calculation_audit ADD CONSTRAINT fk_audit_patient 
            FOREIGN KEY (patient_id) REFERENCES patient_profile(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_egfr_audit_patient ON egfr_calculation_audit(patient_id);
CREATE INDEX IF NOT EXISTS idx_egfr_audit_metrics ON egfr_calculation_audit(consultation_metrics_id);
CREATE INDEX IF NOT EXISTS idx_egfr_audit_timestamp ON egfr_calculation_audit(calculation_timestamp);

COMMENT ON TABLE egfr_calculation_audit IS 'Audit trail of all eGFR calculations for regulatory compliance (ISO 13485)';

-- ============================================================================
-- 4. Create unit_conversion_log table for tracking unit conversions
-- ============================================================================

CREATE TABLE IF NOT EXISTS unit_conversion_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lab_result_id UUID,
    original_value DECIMAL(15, 4) NOT NULL,
    original_unit VARCHAR(20) NOT NULL,
    converted_value DECIMAL(15, 4) NOT NULL,
    converted_unit VARCHAR(20) NOT NULL,
    conversion_factor DECIMAL(15, 6),
    conversion_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_conversion_lab_result FOREIGN KEY (lab_result_id) 
        REFERENCES lab_result(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_conversion_lab_result ON unit_conversion_log(lab_result_id);
CREATE INDEX IF NOT EXISTS idx_conversion_timestamp ON unit_conversion_log(conversion_timestamp);

COMMENT ON TABLE unit_conversion_log IS 'Log of all unit conversions (e.g., mg/dL → µmol/L) for audit and verification';

-- ============================================================================
-- 5. Create ckd_reference_ranges table for clinical guidelines
-- ============================================================================

CREATE TABLE IF NOT EXISTS ckd_reference_ranges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ckd_stage VARCHAR(20) NOT NULL UNIQUE,
    egfr_min DECIMAL(10, 2),
    egfr_max DECIMAL(10, 2),
    description VARCHAR(500),
    clinical_recommendation TEXT,
    followup_interval_weeks INT,
    requires_nephrology BOOLEAN DEFAULT FALSE,
    requires_dialysis BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT ck_stage_name CHECK (ckd_stage IN ('STAGE_1', 'STAGE_2', 'STAGE_3A', 'STAGE_3B', 'STAGE_4', 'STAGE_5'))
);

-- Insert reference data
INSERT INTO ckd_reference_ranges (ckd_stage, egfr_min, egfr_max, description, clinical_recommendation, followup_interval_weeks, requires_nephrology, requires_dialysis) VALUES
    ('STAGE_1', 90, 10000, 'Normal or high kidney function', 'No immediate action. Monitor annually.', 52, FALSE, FALSE),
    ('STAGE_2', 60, 89, 'Mildly decreased kidney function', 'Monitor kidney function. Manage cardiovascular risk factors.', 52, FALSE, FALSE),
    ('STAGE_3A', 45, 59, 'Mildly to moderately decreased kidney function', 'Nephrology referral recommended.', 12, TRUE, FALSE),
    ('STAGE_3B', 30, 44, 'Moderately to severely decreased kidney function', 'Nephrology referral recommended.', 12, TRUE, FALSE),
    ('STAGE_4', 15, 29, 'Severely decreased kidney function', 'Nephrology referral required. Prepare for RRT.', 6, TRUE, TRUE),
    ('STAGE_5', 0, 14.99, 'Kidney failure (Stage 5 CKD)', 'Renal replacement therapy (dialysis/transplant) required.', 2, TRUE, TRUE)
ON CONFLICT (ckd_stage) DO NOTHING;

COMMENT ON TABLE ckd_reference_ranges IS 'Clinical reference ranges and guidelines for CKD stages per KDIGO 2021';

-- ============================================================================
-- 6. Create serum_creatinine_lab_template table for lab requisition standardization
-- ============================================================================

CREATE TABLE IF NOT EXISTS serum_creatinine_lab_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    test_name VARCHAR(255) NOT NULL,
    test_code VARCHAR(50),
    unit VARCHAR(20) DEFAULT 'MICROMOL_L',
    reference_range_min DECIMAL(15, 4),
    reference_range_max DECIMAL(15, 4),
    reference_range_description VARCHAR(255),
    critical_low DECIMAL(15, 4),
    critical_high DECIMAL(15, 4),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert templates
INSERT INTO serum_creatinine_lab_template (name, test_name, test_code, unit, reference_range_min, reference_range_max, reference_range_description, critical_low, critical_high) VALUES
    ('Serum Creatinine (Adult Male)', 'SERUM_CREATININE', 'CREA', 'MICROMOL_L', 60, 110, '60-110 µmol/L', 40, 500),
    ('Serum Creatinine (Adult Female)', 'SERUM_CREATININE', 'CREA', 'MICROMOL_L', 50, 100, '50-100 µmol/L', 40, 500)
ON CONFLICT (name) DO NOTHING;

COMMENT ON TABLE serum_creatinine_lab_template IS 'Standard lab test templates for serum creatinine with reference ranges and critical values';

-- ============================================================================
-- 7. Update existing consultation_metrics records with default values
-- ============================================================================

UPDATE consultation_metrics SET 
    serum_creatinine_unit = 'MICROMOL_L',
    egfr_formula_used = 'CKD_EPI_2021',
    egfr_last_updated_at = updated_at
WHERE serum_creatinine_unit IS NULL;

-- ============================================================================
-- 8. Create indexes for performance optimization
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_consultation_metrics_egfr_trend ON consultation_metrics(egfr_trend);
CREATE INDEX IF NOT EXISTS idx_consultation_metrics_ckd_stage ON consultation_metrics(ckd_stage);
CREATE INDEX IF NOT EXISTS idx_consultation_metrics_updated_at ON consultation_metrics(egfr_last_updated_at);

-- ============================================================================
-- 9. Create views for common queries
-- ============================================================================

-- View: Recent eGFR calculations by patient
CREATE OR REPLACE VIEW recent_egfr_calculations AS
SELECT 
    cm.patient_id,
    cm.consultation_id,
    cm.egfr,
    cm.ckd_stage,
    cm.egfr_change,
    cm.egfr_change_percent,
    cm.egfr_trend,
    cm.egfr_last_updated_at,
    ROW_NUMBER() OVER (PARTITION BY cm.patient_id ORDER BY cm.egfr_last_updated_at DESC) as recency_rank
FROM consultation_metrics cm
WHERE cm.egfr IS NOT NULL;

-- View: High-risk patients (rapid decline or low eGFR)
CREATE OR REPLACE VIEW high_risk_patients AS
SELECT DISTINCT
    cm.patient_id,
    cm.egfr,
    cm.ckd_stage,
    cm.egfr_trend,
    CASE 
        WHEN cm.egfr < 15 THEN 'CRITICAL'
        WHEN cm.egfr < 30 THEN 'HIGH'
        WHEN cm.egfr_trend = 'RAPID_DECLINE' THEN 'HIGH'
        ELSE 'MODERATE'
    END as risk_level
FROM consultation_metrics cm
WHERE cm.egfr IS NOT NULL 
  AND (cm.egfr < 30 OR cm.egfr_trend = 'RAPID_DECLINE')
ORDER BY risk_level DESC, cm.egfr ASC;

-- ============================================================================
-- 10. Add comments and documentation
-- ============================================================================

COMMENT ON SCHEMA public IS 'NephroPaidi Clinical Database - CKD-EPI Formula Implementation';

-- ============================================================================
-- 11. Update schema version tracking
-- ============================================================================

-- Assuming there's a schema_version table (create if it doesn't exist)
CREATE TABLE IF NOT EXISTS schema_version (
    id SERIAL PRIMARY KEY,
    version VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(500),
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO schema_version (version, description) VALUES 
    ('V12_CKD_EPI_2021', 'Added European CKD-EPI formula support with SI units, audit trails, and clinical guidelines')
ON CONFLICT (version) DO NOTHING;

-- ============================================================================
-- Migration complete
-- ============================================================================

-- Verify migration
SELECT 'Migration V12 completed successfully' as status;
SELECT COUNT(*) as table_count FROM information_schema.tables WHERE table_schema = 'public';
