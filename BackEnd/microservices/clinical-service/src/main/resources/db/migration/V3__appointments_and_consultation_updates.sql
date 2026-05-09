-- V3__appointments_and_consultation_updates.sql
-- Add appointment scheduling and consultation linkage

CREATE TABLE appointment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id BIGINT NOT NULL,
    doctor_id UUID NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    duration_minutes INT NOT NULL DEFAULT 30,
    reason TEXT,
    status VARCHAR(32) NOT NULL,
    cancellation_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_appointment_doctor_id ON appointment(doctor_id);
CREATE INDEX idx_appointment_scheduled_at ON appointment(scheduled_at);
CREATE INDEX idx_appointment_status ON appointment(status);

ALTER TABLE appointment
ADD CONSTRAINT chk_appointment_status
CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'CANCELLED', 'NO_SHOW'));

ALTER TABLE consultation
    ADD COLUMN appointment_id UUID,
    ADD COLUMN started_at TIMESTAMP;

ALTER TABLE consultation
    DROP CONSTRAINT IF EXISTS chk_consultation_status;

ALTER TABLE consultation
    ADD CONSTRAINT chk_consultation_status
    CHECK (status IN ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'));

CREATE INDEX idx_consultation_appointment_id ON consultation(appointment_id);
