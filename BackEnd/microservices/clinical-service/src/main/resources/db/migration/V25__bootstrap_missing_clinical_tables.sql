-- V25: Bootstrap missing clinical-service tables and align renamed columns
-- Purpose: Complete schema pieces that were previously only implied by JPA mappings

ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS patient_id UUID;
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS consultation_type VARCHAR(255);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS admission_mode VARCHAR(255);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS referring_physician_id UUID;
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS consultation_date TIMESTAMP;
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS attending_physician_id UUID;
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS chief_complaint TEXT;
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS vital_weight_kg NUMERIC(5, 2);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS vital_height_cm NUMERIC(5, 2);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS vital_head_circumference_cm NUMERIC(5, 2);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS vital_bp_systolic INTEGER;
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS vital_bp_diastolic INTEGER;
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS bp_measurement_limb VARCHAR(255);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS bp_cuff_size VARCHAR(255);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS vital_heart_rate INTEGER;
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS vital_respiratory_rate INTEGER;
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS vital_temperature NUMERIC(4, 1);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS vital_oxygen_saturation INTEGER;
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS edemas_present BOOLEAN;
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS edemas_location VARCHAR(255);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS nutritional_assessment VARCHAR(255);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS soap_subjective TEXT;
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS soap_objective TEXT;
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS soap_assessment TEXT;
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS soap_plan TEXT;
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS nephro_egfr NUMERIC(8, 2);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS nephro_k NUMERIC(4, 2);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS nephro_serum_creatinine_umol_l NUMERIC(8, 2);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS nephro_serum_creatinine_mg_dl NUMERIC(8, 2);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS cystatin_c_mg_l NUMERIC(8, 2);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS nephro_ckd_stage VARCHAR(255);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS nephro_ckd_cause VARCHAR(255);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS urine_protein_creatinine_ratio NUMERIC(8, 2);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS proteinuria_category VARCHAR(255);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS hematuria_level VARCHAR(255);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS dysmorphic_erythrocytes BOOLEAN;
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS leukocyturia BOOLEAN;
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS serum_albumin_g_l NUMERIC(8, 2);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS serum_sodium_mmol_l NUMERIC(8, 2);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS serum_potassium_mmol_l NUMERIC(8, 2);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS serum_bicarbonate_mmol_l NUMERIC(8, 2);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS serum_phosphate_mmol_l NUMERIC(8, 2);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS serum_calcium_mmol_l NUMERIC(8, 2);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS pth_pg_ml NUMERIC(8, 2);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS vitamin_d_25oh_nmol_l NUMERIC(8, 2);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS serum_urea_mmol_l NUMERIC(8, 2);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS hus_present BOOLEAN;
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS hus_type VARCHAR(255);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS aki_present BOOLEAN;
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS aki_origin VARCHAR(255);
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS nephrotic_syndrome BOOLEAN;
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS hereditary_nephropathy BOOLEAN;
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE consultation_records ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_patient_id ON consultation_records(patient_id);
CREATE INDEX IF NOT EXISTS idx_consultation_date ON consultation_records(consultation_date);

CREATE TABLE IF NOT EXISTS consultation_allergies (
    consultation_id UUID NOT NULL,
    allergy_type VARCHAR(255) NOT NULL,
    responsible_agent VARCHAR(255) NOT NULL,
    reaction_type VARCHAR(255),
    severity VARCHAR(255),
    status VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_consultation_allergies_consultation_id ON consultation_allergies(consultation_id);

CREATE TABLE IF NOT EXISTS medical_dossier_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    entry_type VARCHAR(255) NOT NULL,
    summary TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source_service_id UUID
);

CREATE INDEX IF NOT EXISTS idx_medical_dossier_patient_id ON medical_dossier_entries(patient_id);
CREATE INDEX IF NOT EXISTS idx_medical_dossier_created_at ON medical_dossier_entries(created_at);
CREATE INDEX IF NOT EXISTS idx_medical_dossier_entry_type ON medical_dossier_entries(entry_type);

CREATE TABLE IF NOT EXISTS icd10_diagnoses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consultation_id UUID NOT NULL,
    icd10_code VARCHAR(10) NOT NULL,
    diagnosis_label VARCHAR(255) NOT NULL,
    is_primary BOOLEAN,
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_consultation_id ON icd10_diagnoses(consultation_id);

ALTER TABLE technical_acts
    ALTER COLUMN act_name TYPE VARCHAR(255);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'medications_at_discharge'
          AND column_name = 'route_of_administration'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'medications_at_discharge'
          AND column_name = 'route_of_admin'
    ) THEN
        ALTER TABLE medications_at_discharge
            RENAME COLUMN route_of_administration TO route_of_admin;
    END IF;
END $$;
