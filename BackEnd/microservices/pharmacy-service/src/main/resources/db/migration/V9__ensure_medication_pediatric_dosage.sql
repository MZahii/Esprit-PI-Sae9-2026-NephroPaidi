-- Keep existing pharmacy databases compatible with the Medication entity.
-- Some shared/dev databases were created without this column even though the
-- current entity and seed data expect it.
ALTER TABLE medications
    ADD COLUMN IF NOT EXISTS pediatric_dosage VARCHAR(255);
