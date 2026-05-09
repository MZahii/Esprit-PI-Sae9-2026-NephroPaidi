-- V5__clinical_advanced_features.sql
-- Archiving, metrics, medications, audit trail

ALTER TABLE appointment
    ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS archived_by TEXT;

ALTER TABLE consultation
    ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS archived_by TEXT;

ALTER TABLE appointment
    DROP CONSTRAINT IF EXISTS chk_appointment_status;

ALTER TABLE appointment
    ADD CONSTRAINT chk_appointment_status
    CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'CANCELLED', 'NO_SHOW', 'ARCHIVED'));

ALTER TABLE consultation
    DROP CONSTRAINT IF EXISTS chk_consultation_status;

ALTER TABLE consultation
    ADD CONSTRAINT chk_consultation_status
    CHECK (status IN ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'ARCHIVED'));

CREATE TABLE consultation_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consultation_id UUID NOT NULL UNIQUE,
    patient_id BIGINT NOT NULL,
    height_cm DOUBLE PRECISION,
    creatinine_mg_dl DOUBLE PRECISION,
    weight_kg DOUBLE PRECISION,
    age_years INT,
    egfr DOUBLE PRECISION,
    ckd_stage VARCHAR(16),
    alert_low_egfr BOOLEAN DEFAULT FALSE,
    alert_rapid_decline BOOLEAN DEFAULT FALSE,
    alert_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_consultation_metrics_consultation
        FOREIGN KEY (consultation_id) REFERENCES consultation(id) ON DELETE CASCADE
);

CREATE INDEX idx_consultation_metrics_patient_id ON consultation_metrics(patient_id);

CREATE TABLE medication_order (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consultation_id UUID NOT NULL,
    medication_name TEXT NOT NULL,
    dose_mg DOUBLE PRECISION,
    frequency_per_day INT,
    duration_days INT,
    note TEXT,
    weight_kg DOUBLE PRECISION,
    age_years INT,
    validation_status VARCHAR(16),
    validation_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_medication_order_consultation
        FOREIGN KEY (consultation_id) REFERENCES consultation(id) ON DELETE CASCADE
);

CREATE INDEX idx_medication_order_consultation_id ON medication_order(consultation_id);

CREATE TABLE clinical_audit_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(64) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(64) NOT NULL,
    actor_id VARCHAR(128),
    actor_username VARCHAR(128),
    actor_role VARCHAR(64),
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_clinical_audit_entity ON clinical_audit_event(entity_type, entity_id);
