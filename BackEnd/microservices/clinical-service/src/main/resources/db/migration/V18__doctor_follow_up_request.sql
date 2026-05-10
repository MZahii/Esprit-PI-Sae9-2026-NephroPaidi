-- Doctor-initiated follow-up scheduling requests for receptionist confirmation

CREATE TABLE doctor_follow_up_request (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consultation_id UUID NOT NULL REFERENCES consultation(id),
    patient_id BIGINT NOT NULL,
    doctor_id UUID NOT NULL,
    anchor_date DATE NOT NULL,
    offset_amount INT NOT NULL,
    offset_unit VARCHAR(16) NOT NULL,
    computed_return_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_follow_up_offset_unit CHECK (offset_unit IN ('DAYS', 'WEEKS', 'MONTHS')),
    CONSTRAINT chk_follow_up_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED'))
);

CREATE INDEX idx_doctor_follow_up_status ON doctor_follow_up_request(status);
CREATE INDEX idx_doctor_follow_up_consultation ON doctor_follow_up_request(consultation_id);
