-- V2__alter_patient_id_to_bigint.sql
-- Align patient_id with administration-service (BIGINT)

ALTER TABLE consultation
    ALTER COLUMN patient_id TYPE BIGINT
    USING patient_id::text::bigint;
