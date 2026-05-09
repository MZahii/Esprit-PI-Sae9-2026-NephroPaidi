-- V4__consultation_outcomes.sql
-- Consultation outcome payloads (notes, diagnosis, prescriptions, labs, plan)

CREATE TABLE consultation_outcome (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consultation_id UUID NOT NULL UNIQUE,
    notes TEXT,
    diagnosis TEXT,
    prescriptions TEXT,
    lab_requests TEXT,
    treatment_plan TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_consultation_outcome_consultation
        FOREIGN KEY (consultation_id) REFERENCES consultation(id) ON DELETE CASCADE
);

CREATE INDEX idx_consultation_outcome_consultation_id ON consultation_outcome(consultation_id);
