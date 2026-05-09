-- V1__init.sql
-- Create consultation table for clinical service

CREATE TABLE consultation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    date_time TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Create indexes for performance
CREATE INDEX idx_consultation_patient_id ON consultation(patient_id);
CREATE INDEX idx_consultation_doctor_id ON consultation(doctor_id);

-- Add check constraint for status values
ALTER TABLE consultation 
ADD CONSTRAINT chk_consultation_status 
CHECK (status IN ('OPEN', 'COMPLETED', 'CANCELLED'));

-- Add comment for documentation
COMMENT ON TABLE consultation IS 'Clinical consultations with patients and doctors';
COMMENT ON COLUMN consultation.id IS 'Unique consultation identifier';
COMMENT ON COLUMN consultation.patient_id IS 'Reference to patient from patient-service';
COMMENT ON COLUMN consultation.doctor_id IS 'Reference to doctor from user-service';
COMMENT ON COLUMN consultation.date_time IS 'Scheduled consultation time';
COMMENT ON COLUMN consultation.status IS 'Consultation status: OPEN, COMPLETED, or CANCELLED';
