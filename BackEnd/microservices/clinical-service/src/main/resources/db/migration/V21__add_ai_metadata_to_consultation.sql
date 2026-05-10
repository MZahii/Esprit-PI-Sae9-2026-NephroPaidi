-- V21: Add AI Document Metadata to Consultation Records
-- Purpose: Support PDF/Document scanner integration for automated lab data extraction
-- These columns track document quality, parser confidence, and extraction metadata

CREATE TABLE IF NOT EXISTS consultation_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid()
);

ALTER TABLE consultation_records
ADD COLUMN IF NOT EXISTS parser_confidence FLOAT;

ALTER TABLE consultation_records
ADD COLUMN IF NOT EXISTS content_type VARCHAR(100);

ALTER TABLE consultation_records
ADD COLUMN IF NOT EXISTS requires_manual_review BOOLEAN DEFAULT false;

ALTER TABLE consultation_records
ADD COLUMN IF NOT EXISTS document_age_days INTEGER;

ALTER TABLE consultation_records
ADD COLUMN IF NOT EXISTS is_reviewed BOOLEAN DEFAULT false;

ALTER TABLE consultation_records
ADD COLUMN IF NOT EXISTS hospital_id INTEGER DEFAULT 0;

ALTER TABLE consultation_records
ADD COLUMN IF NOT EXISTS department_id INTEGER DEFAULT 0;

ALTER TABLE consultation_records
ADD COLUMN IF NOT EXISTS serum_creatinine_umol_l NUMERIC(8, 2);

ALTER TABLE consultation_records
ADD COLUMN IF NOT EXISTS document_quality_score FLOAT;

-- Index for filtering by review status
CREATE INDEX IF NOT EXISTS idx_requires_manual_review ON consultation_records(requires_manual_review);
CREATE INDEX IF NOT EXISTS idx_is_reviewed ON consultation_records(is_reviewed);
CREATE INDEX IF NOT EXISTS idx_parser_confidence ON consultation_records(parser_confidence);
