ALTER TABLE patient_profiles
    ADD COLUMN IF NOT EXISTS blood_type VARCHAR(8),
    ADD COLUMN IF NOT EXISTS allergies TEXT,
    ADD COLUMN IF NOT EXISTS chronic_conditions TEXT,
    ADD COLUMN IF NOT EXISTS medical_notes TEXT;
