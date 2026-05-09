-- Add extended fields to surgical_cases table for better clinical data tracking
ALTER TABLE surgical_cases
    ADD COLUMN IF NOT EXISTS age INTEGER,
    ADD COLUMN IF NOT EXISTS gender VARCHAR(50),
    ADD COLUMN IF NOT EXISTS medical_record_number VARCHAR(255),
    ADD COLUMN IF NOT EXISTS surgery_type VARCHAR(255),
    ADD COLUMN IF NOT EXISTS procedure_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS surgery_category VARCHAR(100),
    ADD COLUMN IF NOT EXISTS urgency_level VARCHAR(100),
    ADD COLUMN IF NOT EXISTS surgeon_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS assistant_surgeon_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS anesthesiologist_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS nurse_team VARCHAR(255),
    ADD COLUMN IF NOT EXISTS scheduled_date DATE,
    ADD COLUMN IF NOT EXISTS scheduled_start_time TIME,
    ADD COLUMN IF NOT EXISTS estimated_duration_minutes INTEGER,
    ADD COLUMN IF NOT EXISTS operating_room VARCHAR(100);
