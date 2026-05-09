-- V23: Create Discharge Document and Related Tables
-- Purpose: HAS (Hospitalized and School-aged) discharge documentation with 5 mandatory sections

CREATE TABLE IF NOT EXISTS discharge_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    discharge_date TIMESTAMP NOT NULL,
    
    -- HAS §1: Admission Reason
    admission_reason VARCHAR(500),
    
    -- HAS §2: Medical Summary
    medical_summary TEXT,
    
    -- HAS §3: Technical Acts (stored in separate table)
    -- HAS §4: Medications at Discharge (stored in separate table)
    -- HAS §5: Follow-up Plan (embedded/stored in separate table)
    
    discharge_destination VARCHAR(200),
    discharge_weight_g NUMERIC(8, 2),
    
    -- CRH Document Status
    crh_document_status VARCHAR(50),
    guardian_consent_for_dmp BOOLEAN DEFAULT false,
    distribution_list TEXT,
    document_valid_as_crh BOOLEAN DEFAULT false,
    
    finalized BOOLEAN DEFAULT false,
    finalized_at TIMESTAMP,
    
    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_discharge_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_discharge_patient_id ON discharge_documents(patient_id);
CREATE INDEX IF NOT EXISTS idx_discharge_date ON discharge_documents(discharge_date);
CREATE INDEX IF NOT EXISTS idx_discharge_finalized ON discharge_documents(finalized);

-- V23b: Create Technical Acts Table (HAS §3)
CREATE TABLE IF NOT EXISTS technical_acts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    discharge_id UUID NOT NULL REFERENCES discharge_documents(id) ON DELETE CASCADE,
    act_name VARCHAR(200) NOT NULL,
    indication VARCHAR(500),
    act_date TIMESTAMP,
    findings TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_act_discharge FOREIGN KEY (discharge_id) REFERENCES discharge_documents(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tech_act_discharge_id ON technical_acts(discharge_id);

-- V23c: Create Medications at Discharge Table (HAS §4)
CREATE TABLE IF NOT EXISTS medications_at_discharge (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    discharge_id UUID NOT NULL REFERENCES discharge_documents(id) ON DELETE CASCADE,
    medication_name VARCHAR(200) NOT NULL,
    dosage_value NUMERIC(8, 3),
    dosage_unit VARCHAR(50),
    route_of_administration VARCHAR(100),
    frequency VARCHAR(100),
    medication_status VARCHAR(50),
    indication VARCHAR(500),
    modification_justification TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_med_discharge FOREIGN KEY (discharge_id) REFERENCES discharge_documents(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_med_discharge_id ON medications_at_discharge(discharge_id);

-- V23d: Create Follow-up Plan Table (HAS §5)
CREATE TABLE IF NOT EXISTS follow_up_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    discharge_id UUID NOT NULL UNIQUE REFERENCES discharge_documents(id) ON DELETE CASCADE,
    general_practitioner_name VARCHAR(200),
    followup_timeline VARCHAR(200),
    followup_objectives TEXT,
    specialist_referrals TEXT,
    additional_notes TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_followup_discharge FOREIGN KEY (discharge_id) REFERENCES discharge_documents(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_followup_discharge_id ON follow_up_plans(discharge_id);
