CREATE TABLE IF NOT EXISTS dialysis_plans (
    id BIGSERIAL PRIMARY KEY,
    patient_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS dialysis_prescriptions (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    details TEXT,
    CONSTRAINT fk_dialysis_prescription_plan
        FOREIGN KEY (plan_id) REFERENCES dialysis_plans (id)
);

CREATE TABLE IF NOT EXISTS dialysis_sessions (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    session_date TIMESTAMP,
    notes TEXT,
    CONSTRAINT fk_dialysis_session_plan
        FOREIGN KEY (plan_id) REFERENCES dialysis_plans (id)
);

CREATE TABLE IF NOT EXISTS dialysis_outcomes (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    validated BOOLEAN NOT NULL DEFAULT FALSE,
    summary TEXT,
    CONSTRAINT fk_dialysis_outcome_session
        FOREIGN KEY (session_id) REFERENCES dialysis_sessions (id)
);

CREATE TABLE IF NOT EXISTS surgical_cases (
    id BIGSERIAL PRIMARY KEY,
    patient_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    offer_status VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS preop_assessments (
    id BIGSERIAL PRIMARY KEY,
    surgical_case_id BIGINT NOT NULL,
    notes TEXT,
    CONSTRAINT fk_preop_assessment_case
        FOREIGN KEY (surgical_case_id) REFERENCES surgical_cases (id)
);

CREATE TABLE IF NOT EXISTS postop_observations (
    id BIGSERIAL PRIMARY KEY,
    surgical_case_id BIGINT NOT NULL,
    notes TEXT,
    CONSTRAINT fk_postop_observation_case
        FOREIGN KEY (surgical_case_id) REFERENCES surgical_cases (id)
);

CREATE TABLE IF NOT EXISTS surgical_complications (
    id BIGSERIAL PRIMARY KEY,
    surgical_case_id BIGINT NOT NULL,
    description TEXT,
    CONSTRAINT fk_surgical_complication_case
        FOREIGN KEY (surgical_case_id) REFERENCES surgical_cases (id)
);

CREATE TABLE IF NOT EXISTS care_tasks (
    id BIGSERIAL PRIMARY KEY,
    surgical_case_id BIGINT NOT NULL,
    title VARCHAR(255),
    done BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_care_task_case
        FOREIGN KEY (surgical_case_id) REFERENCES surgical_cases (id)
);
