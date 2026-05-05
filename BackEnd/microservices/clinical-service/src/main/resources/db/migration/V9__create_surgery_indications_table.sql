-- V9__create_surgery_indications_table.sql
-- Create surgery_indications table for surgery indication workflow

CREATE TABLE surgery_indications (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL,
    patient_id BIGINT NOT NULL,
    urgency VARCHAR(50) NOT NULL CHECK (urgency IN ('ROUTINE', 'SEMI_URGENT', 'URGENT', 'EMERGENCY')),
    notes TEXT,
    status VARCHAR(50) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACKNOWLEDGED', 'SCHEDULED', 'CANCELLED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_surgery_indications_doctor_id ON surgery_indications(doctor_id);
CREATE INDEX idx_surgery_indications_patient_id ON surgery_indications(patient_id);
CREATE INDEX idx_surgery_indications_status ON surgery_indications(status);
CREATE INDEX idx_surgery_indications_urgency ON surgery_indications(urgency);
CREATE INDEX idx_surgery_indications_created_at ON surgery_indications(created_at DESC);
