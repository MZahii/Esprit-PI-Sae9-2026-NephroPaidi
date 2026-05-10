-- V8__create_lab_requests_table.sql
-- Create lab_requests and lab_results tables for lab workflow

CREATE TABLE lab_requests (
    id UUID PRIMARY KEY,
    doctor_id UUID NOT NULL,
    patient_id BIGINT NOT NULL,
    test_type VARCHAR(500) NOT NULL,
    urgency VARCHAR(50) DEFAULT 'ROUTINE' CHECK (urgency IN ('ROUTINE', 'URGENT', 'STAT')),
    status VARCHAR(50) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_lab_requests_doctor_id ON lab_requests(doctor_id);
CREATE INDEX idx_lab_requests_patient_id ON lab_requests(patient_id);
CREATE INDEX idx_lab_requests_status ON lab_requests(status);
CREATE INDEX idx_lab_requests_created_at ON lab_requests(created_at DESC);

CREATE TABLE lab_results (
    id UUID PRIMARY KEY,
    lab_request_id UUID NOT NULL REFERENCES lab_requests(id) ON DELETE CASCADE,
    file_path VARCHAR(500),
    file_name VARCHAR(255),
    uploaded_by UUID,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_lab_results_lab_request_id ON lab_results(lab_request_id);
CREATE INDEX idx_lab_results_uploaded_at ON lab_results(uploaded_at DESC);
