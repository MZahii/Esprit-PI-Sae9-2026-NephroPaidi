-- V22: Create Hospitalization Records Table
-- Purpose: Track neonatal/pediatric hospitalization episodes with pathology tracking

CREATE TABLE IF NOT EXISTS hospitalization_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    admission_date TIMESTAMP NOT NULL,
    discharge_date TIMESTAMP,
    admission_reason VARCHAR(500),
    
    -- Neonatal specific fields
    birth_weight_g INTEGER,
    gestational_age_at_birth_weeks INTEGER,
    apgar_score_1min INTEGER,
    apgar_score_5min INTEGER,
    
    -- Pathology tracking
    has_respiratory_pathology BOOLEAN DEFAULT false,
    respiratory_pathology_type VARCHAR(100),
    bpd_severity VARCHAR(50),
    
    has_cardiac_pathology BOOLEAN DEFAULT false,
    cardiac_pathology_type VARCHAR(100),
    pda_treatment VARCHAR(100),
    
    intraventricular_hemorrhage VARCHAR(50),
    seizures BOOLEAN DEFAULT false,
    
    -- Kidney status
    ckd_stage VARCHAR(10),
    ckd_cause VARCHAR(200),
    
    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_hosp_patient_id ON hospitalization_records(patient_id);
CREATE INDEX IF NOT EXISTS idx_hosp_admission_date ON hospitalization_records(admission_date);
CREATE INDEX IF NOT EXISTS idx_hosp_discharge_date ON hospitalization_records(discharge_date);
