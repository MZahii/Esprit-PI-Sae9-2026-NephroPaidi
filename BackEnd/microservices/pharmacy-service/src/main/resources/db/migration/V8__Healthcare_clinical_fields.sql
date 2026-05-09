-- V8__Healthcare_clinical_fields.sql
-- Adds clinically required fields to the pharmacy schema for a nephrology context.

-- ─── 1. Medications: rename pediatric_dosage → standard_dosage, add clinical fields ──

ALTER TABLE medications
    RENAME COLUMN pediatric_dosage TO standard_dosage;

ALTER TABLE medications
    ADD COLUMN IF NOT EXISTS generic_name          VARCHAR(255),
    ADD COLUMN IF NOT EXISTS strength              VARCHAR(100),
    ADD COLUMN IF NOT EXISTS unit                  VARCHAR(50),
    ADD COLUMN IF NOT EXISTS therapeutic_class     VARCHAR(100),
    ADD COLUMN IF NOT EXISTS renal_dose_adjustment BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS storage_conditions    VARCHAR(50) NOT NULL DEFAULT 'ROOM_TEMPERATURE',
    ADD COLUMN IF NOT EXISTS controlled_substance  BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN medications.generic_name         IS 'INN (International Nonproprietary Name)';
COMMENT ON COLUMN medications.strength             IS 'e.g. 500 mg, 5 mg/mL, 40 mg/2 mL';
COMMENT ON COLUMN medications.unit                 IS 'e.g. tablet, mL, unit, sachet';
COMMENT ON COLUMN medications.therapeutic_class    IS 'e.g. Diuretic, Immunosuppressant, Phosphate Binder';
COMMENT ON COLUMN medications.renal_dose_adjustment IS 'TRUE if dose must be adjusted for renal impairment';
COMMENT ON COLUMN medications.storage_conditions   IS 'ROOM_TEMPERATURE | REFRIGERATED_2_8 | FROZEN | LIGHT_PROTECTED';
COMMENT ON COLUMN medications.controlled_substance IS 'TRUE if classified as a controlled/narcotic substance';

-- ─── 2. Dispensation logs: add patient & pharmacist traceability ──────────────

ALTER TABLE dispensation_logs
    ADD COLUMN IF NOT EXISTS patient_id     BIGINT,
    ADD COLUMN IF NOT EXISTS patient_name   VARCHAR(255),
    ADD COLUMN IF NOT EXISTS prescription_id BIGINT,
    ADD COLUMN IF NOT EXISTS medication_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS batch_number   VARCHAR(100),
    ADD COLUMN IF NOT EXISTS dispensed_by   VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_dispensation_patient ON dispensation_logs(patient_id);
CREATE INDEX IF NOT EXISTS idx_dispensation_prescription ON dispensation_logs(prescription_id);

-- ─── 3. Pharmacy prescriptions: add urgency, pharmacist verification ─────────

ALTER TABLE pharmacy_prescriptions
    ADD COLUMN IF NOT EXISTS urgency          VARCHAR(20) NOT NULL DEFAULT 'ROUTINE'
        CHECK (urgency IN ('STAT', 'URGENT', 'ROUTINE')),
    ADD COLUMN IF NOT EXISTS verified_by      VARCHAR(255),
    ADD COLUMN IF NOT EXISTS verified_at      TIMESTAMP,
    ADD COLUMN IF NOT EXISTS allergy_confirmed BOOLEAN;

CREATE INDEX IF NOT EXISTS idx_prescriptions_urgency ON pharmacy_prescriptions(urgency);
