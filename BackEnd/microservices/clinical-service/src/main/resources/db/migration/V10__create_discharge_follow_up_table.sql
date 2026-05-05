-- V10__create_discharge_follow_up_table.sql
-- Create discharge_follow_ups and follow_up_items tables for discharge follow-up workflow

CREATE TABLE discharge_follow_ups (
    id UUID PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id UUID NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_discharge_follow_ups_patient_id ON discharge_follow_ups(patient_id);
CREATE INDEX idx_discharge_follow_ups_doctor_id ON discharge_follow_ups(doctor_id);
CREATE INDEX idx_discharge_follow_ups_status ON discharge_follow_ups(status);
CREATE INDEX idx_discharge_follow_ups_created_at ON discharge_follow_ups(created_at DESC);

CREATE TABLE follow_up_items (
    id UUID PRIMARY KEY,
    follow_up_id UUID NOT NULL REFERENCES discharge_follow_ups(id) ON DELETE CASCADE,
    item_type VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    frequency VARCHAR(100),
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    status VARCHAR(50) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_follow_up_items_follow_up_id ON follow_up_items(follow_up_id);
CREATE INDEX idx_follow_up_items_status ON follow_up_items(status);
