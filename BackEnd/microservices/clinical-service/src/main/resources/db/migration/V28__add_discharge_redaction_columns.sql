-- V28: Add discharge document redaction audit columns

ALTER TABLE discharge_documents
    ADD COLUMN IF NOT EXISTS redactor_id UUID,
    ADD COLUMN IF NOT EXISTS redaction_date TIMESTAMP;
