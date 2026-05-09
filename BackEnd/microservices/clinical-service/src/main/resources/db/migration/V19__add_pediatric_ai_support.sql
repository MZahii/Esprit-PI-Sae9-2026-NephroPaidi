-- V19__add_pediatric_ai_support.sql
-- Adds consultation linkage for lab requests plus AI recommendation fields.

ALTER TABLE lab_requests
    ADD COLUMN IF NOT EXISTS consultation_id UUID;

CREATE INDEX IF NOT EXISTS idx_lab_requests_consultation_id
    ON lab_requests(consultation_id);

ALTER TABLE lab_results
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(100),
    ADD COLUMN IF NOT EXISTS file_size_bytes BIGINT,
    ADD COLUMN IF NOT EXISTS ai_status VARCHAR(50),
    ADD COLUMN IF NOT EXISTS ai_recommendation VARCHAR(50),
    ADD COLUMN IF NOT EXISTS ai_confidence NUMERIC,
    ADD COLUMN IF NOT EXISTS ai_summary TEXT,
    ADD COLUMN IF NOT EXISTS ai_requires_doctor_review BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS extracted_creatinine_mg_dl NUMERIC,
    ADD COLUMN IF NOT EXISTS extracted_creatinine_umol NUMERIC,
    ADD COLUMN IF NOT EXISTS extracted_unit VARCHAR(50),
    ADD COLUMN IF NOT EXISTS parser_confidence NUMERIC,
    ADD COLUMN IF NOT EXISTS ai_raw_response TEXT,
    ADD COLUMN IF NOT EXISTS processed_at TIMESTAMP;

ALTER TABLE consultation_metrics
    ADD COLUMN IF NOT EXISTS patient_sex VARCHAR(10),
    ADD COLUMN IF NOT EXISTS ai_recommendation VARCHAR(50),
    ADD COLUMN IF NOT EXISTS ai_confidence NUMERIC,
    ADD COLUMN IF NOT EXISTS ai_summary TEXT,
    ADD COLUMN IF NOT EXISTS ai_requires_review BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS ai_source_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ai_updated_at TIMESTAMP;
