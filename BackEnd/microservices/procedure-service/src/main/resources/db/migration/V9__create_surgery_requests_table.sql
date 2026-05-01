CREATE TABLE IF NOT EXISTS surgery_requests (
    id BIGSERIAL PRIMARY KEY,
    patient_id VARCHAR(255) NOT NULL,
    consultation_id VARCHAR(255) NOT NULL,
    requested_by_doctor_id VARCHAR(255) NOT NULL,
    patient_first_name VARCHAR(255) NOT NULL,
    patient_last_name VARCHAR(255) NOT NULL,
    reason TEXT NOT NULL,
    urgency_level VARCHAR(32) NOT NULL,
    clinical_note TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
