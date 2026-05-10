ALTER TABLE hospitalization_case
    ADD COLUMN IF NOT EXISTS room_number VARCHAR(64);

ALTER TABLE hospitalization_case
    ADD COLUMN IF NOT EXISTS bed_number VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_hospitalization_case_room_bed
    ON hospitalization_case(room_number, bed_number);
