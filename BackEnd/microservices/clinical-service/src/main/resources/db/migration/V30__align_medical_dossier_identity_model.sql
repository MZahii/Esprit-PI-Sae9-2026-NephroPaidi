-- V30: Align medical dossier identity with canonical patient profile IDs.
-- The doctor-facing consultation module uses BIGINT patient IDs, while older
-- dossier events were stored directly against UUID patient IDs. Preserve those
-- legacy rows in a dedicated column and introduce BIGINT patient_id as the
-- canonical key for new dossier queries.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'medical_dossier_entries'
          AND column_name = 'patient_id'
          AND udt_name = 'uuid'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'medical_dossier_entries'
          AND column_name = 'legacy_patient_uuid'
    ) THEN
        ALTER TABLE medical_dossier_entries
            RENAME COLUMN patient_id TO legacy_patient_uuid;
    END IF;
END $$;

ALTER TABLE medical_dossier_entries
    ADD COLUMN IF NOT EXISTS patient_id BIGINT;

ALTER TABLE medical_dossier_entries
    ADD COLUMN IF NOT EXISTS legacy_patient_uuid UUID;

DROP INDEX IF EXISTS idx_medical_dossier_patient_id;
DROP INDEX IF EXISTS idx_patient_id;

CREATE INDEX IF NOT EXISTS idx_medical_dossier_patient_id
    ON medical_dossier_entries(patient_id);

CREATE INDEX IF NOT EXISTS idx_medical_dossier_legacy_patient_uuid
    ON medical_dossier_entries(legacy_patient_uuid);
