ALTER TABLE dialysis_plans
    ADD COLUMN IF NOT EXISTS first_name VARCHAR(255);

ALTER TABLE dialysis_plans
    ADD COLUMN IF NOT EXISTS last_name VARCHAR(255);

ALTER TABLE surgical_cases
    ADD COLUMN IF NOT EXISTS first_name VARCHAR(255);

ALTER TABLE surgical_cases
    ADD COLUMN IF NOT EXISTS last_name VARCHAR(255);
